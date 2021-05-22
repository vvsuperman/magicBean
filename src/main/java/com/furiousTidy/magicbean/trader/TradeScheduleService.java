package com.furiousTidy.magicbean.trader;

import com.binance.api.client.domain.OrderStatus;
import com.binance.api.client.domain.account.NewOrderResponse;
import com.binance.api.client.domain.account.NewOrderResponseType;
import com.binance.api.client.domain.account.request.CancelOrderRequest;
import com.binance.api.client.domain.account.request.OrderStatusRequest;
import com.binance.client.model.enums.NewOrderRespType;
import com.binance.client.model.enums.OrderSide;
import com.binance.client.model.enums.OrderType;
import com.binance.client.model.trade.AccountInformation;
import com.binance.client.model.trade.Order;
import com.furiousTidy.magicbean.apiproxy.ProxyUtil;
import com.furiousTidy.magicbean.apiproxy.SpotSyncClientProxy;
import com.furiousTidy.magicbean.config.BeanConfig;
import com.furiousTidy.magicbean.dbutil.dao.PairsTradeDao;
import com.furiousTidy.magicbean.dbutil.dao.TradeInfoDao;
import com.furiousTidy.magicbean.dbutil.model.PairsTradeModel;
import com.furiousTidy.magicbean.influxdb.InfluxDbConnection;
import com.furiousTidy.magicbean.subscription.PreTradeService;
import com.furiousTidy.magicbean.trader.service.AfterOrderService;
import com.furiousTidy.magicbean.trader.service.PositionOpenService;
import com.furiousTidy.magicbean.util.BeanConstant;
import com.furiousTidy.magicbean.util.BinanceClient;
import com.furiousTidy.magicbean.util.MarketCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import com.binance.api.client.domain.TransferType;

import static com.binance.api.client.domain.account.NewOrder.marketBuy;
import static com.binance.api.client.domain.account.NewOrder.marketSell;


// schedule service
@Configuration
@EnableScheduling
@Slf4j
public class TradeScheduleService {

    @Autowired
    public PairsTradeDao pairsTradeDao;

    @Autowired
    public TradeInfoDao tradeInfoDao;

    @Autowired
    TradeUtil tradeUtil;

    @Autowired
    SpotSyncClientProxy spotSyncClientProxy;

    @Autowired
    ProxyUtil proxyUtil;

    @Autowired
    PreTradeService preTradeService;

    @Autowired
    InfluxDbConnection influxDbConnection;

    @Autowired
    AfterOrderService afterOrderService;

    @Autowired
    PositionOpenService positionOpenService;

    @Autowired
    BinanceClient binanceClient;

    @Value("${accountName}")
    String accountName;

    @Autowired
    MarketCache marketCache;

    //get all the open pairs trade for close
    @Scheduled(cron = "0 0/5 * * * ?")
    public void getAllOpenOrder(){
        BeanConstant.pairsTradeList =  pairsTradeDao.getPairsTradeOpen();
        //store trade_info in the map;

        BeanConstant.pairsTradeList.forEach(pairsTradeModel ->{
            // some bug for closeid
//            if(pairsTradeModel.getCloseId()!=null){
//                   pairsTradeDao.setCloseId2Null( pairsTradeModel.getId());
//            }
            BeanConstant.tradeInfoMap.put(pairsTradeModel.getOpenId()
                    ,tradeInfoDao.getTradeInfoByOrderId(pairsTradeModel.getOpenId()));
            }

        );

        //sort the list min to high
        positionOpenService.sortPairsTradeList(BeanConstant.pairsTradeList);
    }



    @Scheduled(cron = "0 0 1 * * ?")
    public void closedTradeNDaysAgo() throws InterruptedException {
        LocalDate closeDay = LocalDate.now().minusDays(BeanConfig.N_DAY);
        String year = closeDay.getYear()+"";
        String month = closeDay.getMonthValue()>=10?closeDay.getMonthValue()+"":"0"+closeDay.getMonthValue();
        String day = closeDay.getDayOfMonth()>=10 ? closeDay.getDayOfMonth()+"":"0"+closeDay.getDayOfMonth();
        String strDay = year+"/"+month+"/"+day;
        log.info("strday..............{}", strDay);
        List<PairsTradeModel> pairsTradeModels = pairsTradeDao.getPairsTradeOpenByDate(strDay);
        log.info("pairstrademodels................{}",pairsTradeModels);
        tradeUtil.closePairsTradeList(pairsTradeModels);
    }



    // query all manul close order's status
    @Scheduled(cron = "0 0/5 * * * ?")
    public void queryAndUpdateOrder(){
        for(Map.Entry <String, String> entry :MarketCache.futureOrderCache.entrySet()){
            String clientOrderId = entry.getKey();
            String symbol = entry.getValue();
            log.info("before get future: clientOrderid={},symbol={}", clientOrderId, symbol);
            Order order;
            try{
                 order = binanceClient.getFutureSyncClient().getOrder(symbol,null,clientOrderId);
            }catch (Exception ex){
                if(ex.getMessage().contains("Order does not exist")){
                    marketCache.deleteOrder(clientOrderId,"future");
                }
                log.info("get future exception: clientOrderId={}, ex={}",clientOrderId, ex);
                continue;
            }

            log.info("after get futrure  order info:clientOrderid={}, price={}, qty={}, order={}",clientOrderId,order.getPrice(),order.getExecutedQty(), order);
            if(order.getStatus().equals("FILLED")){
                afterOrderService.processFutureOrder(symbol,clientOrderId,order.getPrice(),order.getExecutedQty(),null,-1);
                marketCache.deleteOrder(order.getClientOrderId(),"future");
            }else if(forceCloseCheckFuture(order)){
                log.info("force close future order:clientOrderid={}, price={},currentPrice={}, qty={}, order={}",clientOrderId,order.getPrice()
                        ,MarketCache.futureTickerMap.get(symbol).getBestAskPrice(),order.getExecutedQty(), order);

                binanceClient.getFutureSyncClient().cancelOrder(symbol,null,clientOrderId);
                doFutureMarketOrder(order);
                marketCache.deleteOrder(clientOrderId,"future");

            }
        }

        for(Map.Entry <String, String> entry :MarketCache.spotOrderCache.entrySet()){
            String clientOrderId = entry.getKey();
            String symbol = entry.getValue();

            log.info("before get spot: clientOrderid={},symbol={}", clientOrderId, symbol);

            com.binance.api.client.domain.account.Order order;
            try{
                order = binanceClient.getSpotSyncClient().getOrderStatus(new OrderStatusRequest(symbol,clientOrderId));
            }catch (Exception ex){
                if(ex.getMessage().contains("Order does not exist")){
                    marketCache.deleteOrder(clientOrderId,"spot");
                }
                log.info("get spot order clientOrderId={},exception={}",clientOrderId,ex);
                continue;
            }

            log.info("get spot order info:clientOrderid={}, price={}, currentprice={},current qty={}, order={}",clientOrderId,order.getPrice()
                    ,MarketCache.spotTickerMap.get(symbol).getAskPrice(),order.getExecutedQty(),order);
            if(order.getStatus() == OrderStatus.FILLED){
                afterOrderService.processSpotOrder(symbol, clientOrderId, new BigDecimal(order.getPrice()), new BigDecimal(order.getExecutedQty()),null,-1);
                marketCache.deleteOrder(order.getClientOrderId(),"spot");
            }else if(forceCloseCheckSpot(order)){
                log.info(" force close for spot order: clientOrderid={},order={}",clientOrderId,order);
                binanceClient.getSpotSyncClient().cancelOrder(new CancelOrderRequest(symbol,clientOrderId));
                doSpotMarketOrder(order);
                marketCache.deleteOrder(clientOrderId,"spot");
            }
        }
    }

    private void doSpotMarketOrder(com.binance.api.client.domain.account.Order order) {
      String clientOrderId = order.getClientOrderId();
      String symbol = order.getSymbol();
      String spotQty = order.getOrigQty();

        NewOrderResponse newOrderResponse = null;
        try{
            if(clientOrderId.contains(BeanConstant.FUTURE_SELL_OPEN)){
                newOrderResponse = spotSyncClientProxy.newOrder(
                        marketBuy(symbol, spotQty.toString()).newOrderRespType(NewOrderResponseType.FULL).newClientOrderId(clientOrderId));
            }else if(clientOrderId.contains(BeanConstant.FUTURE_SELL_CLOSE)){
                newOrderResponse = spotSyncClientProxy.newOrder(
                        marketSell(symbol,spotQty.toString()).newOrderRespType(NewOrderResponseType.FULL).newClientOrderId(clientOrderId));
            }
        }catch (Exception ex){
            log.info("force close spot error: exception ={}",ex);

        }

        afterOrderService.processSpotOrder(symbol,clientOrderId,new BigDecimal(newOrderResponse.getFills().get(0).getPrice())
                ,new BigDecimal(newOrderResponse.getExecutedQty()),new BigDecimal(-1),0);


    }

    private boolean forceCloseCheckSpot(com.binance.api.client.domain.account.Order order) {
        BigDecimal currentPrice = MarketCache.spotTickerMap.get(order.getSymbol()).getBidPrice();
        BigDecimal price = new BigDecimal(order.getPrice());
        if(price.subtract(currentPrice).divide(price,4,RoundingMode.HALF_UP).abs().compareTo(new BigDecimal("0.1"))>0){
            return true;
        }
        return false;
    }

    private boolean forceCloseCheckFuture(Order order) {
        BigDecimal currentPrice = MarketCache.futureTickerMap.get(order.getSymbol()).getBestBidPrice();
        if(order.getPrice().subtract(currentPrice).divide(order.getPrice(),4,RoundingMode.HALF_UP).abs().compareTo(new BigDecimal("0.1"))>0){
            return true;
        }
        return false;
    }

    private void doFutureMarketOrder(Order order) {

        OrderSide orderSide =(order.getClientOrderId().contains(BeanConstant.FUTURE_SELL_OPEN))? OrderSide.SELL:OrderSide.BUY;

        try{

           order = binanceClient.getFutureSyncClient().postOrder(order.getSymbol(),orderSide,null, OrderType.MARKET, null,order.getOrigQty().toString(),
                   null,null,order.getClientOrderId(),null,null, NewOrderRespType.RESULT);
       }catch (Exception ex){
           log.info("force close future error: exception ={}",ex);
       }

       log.info("market order return order={}", order);
        afterOrderService.processFutureOrder(order.getSymbol(),order.getClientOrderId(),order.getAvgPrice(),order.getExecutedQty(), BigDecimal.valueOf(-1),0);

    }


    @Scheduled(cron = "0 0 1 * * ?")
    public void changeLeverageLevel(){
        preTradeService.changeLeverageLevel(1);
    }

    //change gap according to future rate, set
    @Scheduled(cron = "0 0/10 * * * ?")
    public void changePairsGap(){
        MarketCache.futureRateCache.entrySet().stream().filter(entry -> entry.getKey().contains("USDT")).forEach(entry ->{
            MarketCache.pairsGapCache.put(entry.getKey()
                    ,entry.getValue().multiply(BigDecimal.valueOf(2)).subtract(BeanConfig.GAP_FACTOR).compareTo(
                            BeanConfig.OPEN_PRICE_GAP)>0
                            ? entry.getValue().multiply(BigDecimal.valueOf(2)).subtract(BeanConfig.GAP_FACTOR)
                            :BeanConfig.OPEN_PRICE_GAP);
                }
        );

        BeanConstant.GAP_2_BIG = false;
    }

    //future and spot balance and synchronize the local balance with the binance exchange
    // clear the event lock list
    @Scheduled(cron = "0 0/5 * * * ?")
    public void doFutureSpotBalance() throws InterruptedException {
        if(!BeanConstant.watchdog) return;
        BeanConstant.watchdog =false;
        // sleep for 5 second, let on the way order finished, avoid incorrect balance in exchange cache
        Thread.sleep(5000);
        final BigDecimal[] balances = new BigDecimal[2];
        AccountInformation accountInformation = binanceClient.getFutureSyncClient().getAccountInformation();
        accountInformation.getAssets().stream().filter(asset -> asset.getAsset().equals("USDT")).forEach(asset -> {
           balances[0] = asset.getMaxWithdrawAmount();
        });

        binanceClient.getSpotSyncClient().getAccount().getBalances().stream().filter(assetBalance -> assetBalance.getAsset().equals("USDT"))
                .forEach(assetBalance -> {
                    balances[1] = new BigDecimal(assetBalance.getFree());
                });
        log.info("before do balance origin future balance={}, spot balance={},real future balance={}, spot balance={}"
                ,MarketCache.futureBalance.get(),MarketCache.spotBalance.get(), balances[0],balances[1]);


        if(balances[0].subtract(balances[1]).compareTo(BeanConfig.STANDARD_TRADE_UNIT)>0){
            BigDecimal transferUSDT = balances[0].subtract(balances[1]).divide(new BigDecimal(2),2,RoundingMode.HALF_DOWN);
            if(transferUSDT.compareTo(BigDecimal.ZERO)==0) return;
            binanceClient.getMarginRestClient().transfer("USDT",transferUSDT.toString(),TransferType.UMFUTURE_MAIN);
            //synchronize local cache
            MarketCache.futureBalance.set(balances[0].subtract(transferUSDT));
            MarketCache.spotBalance.set(balances[1].add(transferUSDT));

        }else if(balances[1].subtract(balances[0]).compareTo(BeanConfig.STANDARD_TRADE_UNIT)>0){


            BigDecimal transferUSDT = balances[1].subtract(balances[0]).divide(new BigDecimal(2),2,RoundingMode.HALF_DOWN);
            if(transferUSDT.compareTo(BigDecimal.ZERO)==0) return;
            binanceClient.getMarginRestClient().transfer("USDT",transferUSDT.toString(),TransferType.MAIN_UMFUTURE);
            //synchronize local cache
            MarketCache.futureBalance.set(balances[0].add(transferUSDT));
            MarketCache.spotBalance.set(balances[1].subtract(transferUSDT));

        }else{

            MarketCache.futureBalance.set(balances[0]);
            MarketCache.spotBalance.set(balances[1]);
        }

        log.info("after do balance origin future balance={}, spot balance={},real future balance={}, spot balance={}"
                ,MarketCache.futureBalance.get(),MarketCache.spotBalance.get(), balances[0],balances[1]);


        //clear the eventLock
        MarketCache.eventLockCache.clear();

        //spot account will take a delay for transfer money
        Thread.sleep(30000);
        BeanConstant.watchdog =true;
    }

    //get all balance
    @Scheduled(cron = "0 0 2 * * ?")
    public BigDecimal getAllBalance(){
        final BigDecimal[] spotBalance = new BigDecimal[1];
        spotBalance[0] = BigDecimal.ZERO;
        BigDecimal futureBalance = binanceClient.getFutureSyncClient().getAccountInformation().getTotalWalletBalance();
        binanceClient.getSpotSyncClient().getAccount().getBalances()
                .stream()
                .filter(assetBalance -> new BigDecimal(assetBalance.getFree()).compareTo(BigDecimal.ZERO)>0)
                .forEach(assetBalance -> {
                    String symbol = assetBalance.getAsset()+"USDT";
                    if(MarketCache.spotTickerMap.containsKey(symbol)){
                        BigDecimal askPrice = MarketCache.spotTickerMap.get(symbol).getAskPrice();
                        BigDecimal bidPrice = MarketCache.spotTickerMap.get(symbol).getBidPrice();
                        spotBalance[0] = spotBalance[0].add(new BigDecimal(assetBalance.getFree()).multiply(askPrice.add(bidPrice).divide(new BigDecimal(2))));
                    }else if(assetBalance.getAsset().equals("USDT")){
                        spotBalance[0] = spotBalance[0].add(new BigDecimal(assetBalance.getFree()));
                    }

                    else{
                        log.info("no symbol info, symbol={}",symbol);
                    }

        });

        //store into database
        log.info("spotbalance={}, futurebalance={}",spotBalance[0],futureBalance);
        Map<String,String> tagMap = new HashMap<>();
        Map<String,Object> fileMap = new HashMap<>();

        tagMap.put("name", accountName);
        fileMap.put("balance",spotBalance[0].add(futureBalance));

        influxDbConnection.insert("balance_info",tagMap,fileMap);

        return spotBalance[0].add(futureBalance);
    }

    //buy some bnb for exchange charge, check for 1 in the morning
    @Scheduled(cron = "0 0 1 * * ?")
    public void buyBNB(){
        log.info("buy some bnb for exchange charge");
        AccountInformation accountInformation =  binanceClient.getFutureSyncClient().getAccountInformation();
        final BigDecimal[] balances = new BigDecimal[2];
        accountInformation.getAssets().stream().filter(asset -> asset.getAsset().equals("BNB")).forEach(asset -> {
            balances[0] = asset.getMaxWithdrawAmount();
        });

        binanceClient.getSpotSyncClient().getAccount().getBalances().stream().filter(assetBalance -> assetBalance.getAsset().equals("BNB"))
                .forEach(assetBalance -> {
                    balances[1] = new BigDecimal(assetBalance.getFree());
                });
        BigDecimal bnbPrice = MarketCache.spotTickerMap.get("BNBUSDT").getAskPrice();
        Integer[] stepSize = tradeUtil.getStepSize("BNBUSDT");

        log.info("future bnb={}, spot bnb={}", balances[0],balances[1]);

        // buy some bnb
        if(balances[1].multiply(bnbPrice).compareTo(new BigDecimal(10))<0){
            BigDecimal bnbQty = new BigDecimal("15").divide(bnbPrice,stepSize[1],RoundingMode.HALF_UP);
            NewOrderResponse order = spotSyncClientProxy.newOrder(
                    marketBuy("BNBUSDT",
                            bnbQty.toString()).newOrderRespType(NewOrderResponseType.FULL));
        }

        //transfer some bnb to u coin
        if(balances[0].multiply(bnbPrice).compareTo(new BigDecimal(10))<0){
            binanceClient.getMarginRestClient().transfer("BNB",balances[1].divide(new BigDecimal(2),4,RoundingMode.HALF_DOWN).toString(),TransferType.MAIN_UMFUTURE);
        }


    }

    public static void main(String[] args){
        TradeScheduleService tradeScheduleService = new TradeScheduleService();

    }



//    public void subscribeSymbolTick(){
//        BinanceClient.futureSubsptClient.subscribeSymbolBookTickerEvent();
//    }

    /*
    * schedule move tradeInfo and pairsTrade to his_table
    *  if tradeinfo has closeratio and not change between 10s
    * */
    public void bakTradeInfo() throws InterruptedException {
//        List<PairsTradeModel> pairsTradeList = pairsTradeDao.findAllPairsTrade();
//        Thread.sleep(10000);
//        List<PairsTradeModel> pairsTradeList10 = pairsTradeDao.findAllPairsTrade();
//        for(PairsTradeModel pairsTrade: pairsTradeList){
//            for(PairsTradeModel pairsTrade10: pairsTradeList10){
//                if(pairsTrade.getId().equals(pairsTrade10.getId())
//                        && pairsTrade.getCloseRatio()!=null
//                        && pairsTrade.getCloseRatio().equals(pairsTrade10.getCloseRatio())){
//                    pairsTradeHisDao.insertPairsTrade(pairsTrade);
//                    pairsTradeDao.deletePairsTrade(pairsTrade);
//                    TradeInfoModel tradeInfo = tradeInfoDao.getTradeInfoByOpenId(pairsTrade.getOpenId());
//                    tradeInfoHisDao.insertTradeInfo(tradeInfo);
//                    tradeInfoDao.deleteTradeInfo(tradeInfo);
//                }
//            }
//        }

    }

}
