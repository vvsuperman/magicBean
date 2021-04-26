package com.furiousTidy.magicbean.trader;

import com.binance.api.client.domain.OrderStatus;
import com.binance.api.client.domain.account.NewOrderResponse;
import com.binance.api.client.domain.account.NewOrderResponseType;
import com.binance.api.client.domain.account.request.OrderStatusRequest;
import com.binance.client.model.trade.AccountInformation;
import com.binance.client.model.trade.Order;
import com.furiousTidy.magicbean.apiproxy.ProxyUtil;
import com.furiousTidy.magicbean.apiproxy.SpotSyncClientProxy;
import com.furiousTidy.magicbean.config.BeanConfig;
import com.furiousTidy.magicbean.dbutil.dao.PairsTradeDao;
import com.furiousTidy.magicbean.dbutil.dao.TradeInfoDao;
import com.furiousTidy.magicbean.influxdb.InfluxDbConnection;
import com.furiousTidy.magicbean.subscription.PreTradeService;
import com.furiousTidy.magicbean.trader.service.AfterOrderService;
import com.furiousTidy.magicbean.trader.service.PositionOpenService;
import com.furiousTidy.magicbean.util.BeanConstant;
import com.furiousTidy.magicbean.util.BinanceClient;
import com.furiousTidy.magicbean.util.MarketCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import com.binance.api.client.domain.TransferType;

import static com.binance.api.client.domain.account.NewOrder.marketBuy;


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

    //get all the open pairs trade for close
    @Scheduled(cron = "0 0/5 * * * ?")
    public void getAllOpenOrder(){
        BeanConstant.pairsTradeList =  pairsTradeDao.getPairsTradeOpen();
        //store trade_info in the map;

        BeanConstant.pairsTradeList.forEach(pairsTradeModel ->{
            BeanConstant.tradeInfoMap.put(pairsTradeModel.getOpenId()
                    ,tradeInfoDao.getTradeInfoByOrderId(pairsTradeModel.getOpenId()));
            }

        );

        //sort the list min to high
        positionOpenService.sortPairsTradeList(BeanConstant.pairsTradeList);
    }

    @Scheduled(cron = "0 0/5 * * * ?")
    public void queryOrder(){
        for(Map.Entry <String, String> entry :MarketCache.futureOrderCache.entrySet()){
            String clientOrderId = entry.getKey();
            String symbol = entry.getValue();
            Order order = BinanceClient.futureSyncClient.getOrder(symbol,null,clientOrderId);
            log.info("get futrure  order info:clientOrderid={}, price={}, qty={}, order={}",clientOrderId,order.getPrice(),order.getExecutedQty(), order);
            if(order.getStatus().equals("FILLED")){
                afterOrderService.processFutureOrder(symbol,clientOrderId,order.getPrice(),order.getExecutedQty());
                MarketCache.futureOrderCache.remove(order.getClientOrderId());
            }
        }

        for(Map.Entry <String, String> entry :MarketCache.spotOrderCache.entrySet()){
            String clientOrderId = entry.getKey();
            String symbol = entry.getValue();
            com.binance.api.client.domain.account.Order order = BinanceClient.spotSyncClient.getOrderStatus(new OrderStatusRequest(symbol,clientOrderId));
            log.info("get spot order info:clientOrderid={}, price={}, qty={}, order={}",clientOrderId,order.getPrice(),order.getExecutedQty(),order);
            if(order.getStatus() == OrderStatus.FILLED){
                afterOrderService.processSpotOrder(symbol, clientOrderId, new BigDecimal(order.getPrice()), new BigDecimal(order.getExecutedQty()));
                MarketCache.spotOrderCache.remove(order.getClientOrderId());
            }
        }
    }

    //checkNetWork state  test order has no use
//    @Scheduled(cron = "0 0/10 * * * ?")
//    public void checkNetWork() throws InterruptedException {
//        long duration = 0;
//        int n = 5;
//        for(int i=0;i<n;i++){
//            long start = System.currentTimeMillis();
//            BinanceClient.spotSyncClient.newOrderTest(marketBuy("BTCUSDT", "0.001").newOrderRespType(NewOrderResponseType.FULL));
//            duration += System.currentTimeMillis()-start;
//            Thread.sleep(10);
//        }
//
//        if(duration/n > 50){
//            BeanConstant.NETWORK_DELAYED = true;
//            log.info("network delayed, duration={}",duration/n);
//        }else {
//            BeanConstant.NETWORK_DELAYED = false;
//            log.info("network Ok, duration={}",duration/n);
//        }
//
//    }
    @Scheduled(cron = "0 0 1 * * ?")
    public void changeLeverageLevel(){
        preTradeService.changeLeverageLevel(1);
    }

    //change gap according to future rate
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
    }

    //future and spot balance and synchronize the local balance with the binance exchange
    @Scheduled(cron = "0 0/5 * * * ?")
    public void doFutureSpotBalance() throws InterruptedException {
        BeanConstant.watchdog =false;
        // sleep for 5 second, let on the way order finished, avoid incorrect balance in exchange cache
        Thread.sleep(5000);
        final BigDecimal[] balances = new BigDecimal[2];
        AccountInformation accountInformation = BinanceClient.futureSyncClient.getAccountInformation();
        accountInformation.getAssets().stream().filter(asset -> asset.getAsset().equals("USDT")).forEach(asset -> {
           balances[0] = asset.getMaxWithdrawAmount();
        });

        BinanceClient.spotSyncClient.getAccount().getBalances().stream().filter(assetBalance -> assetBalance.getAsset().equals("USDT"))
                .forEach(assetBalance -> {
                    balances[1] = new BigDecimal(assetBalance.getFree());
                });
        log.info("before do balance origin future balance={}, spot balance={},real future balance={}, spot balance={}"
                ,MarketCache.futureBalance.get(),MarketCache.spotBalance.get(), balances[0],balances[1]);


        if(balances[0].subtract(balances[1]).compareTo(BigDecimal.ZERO)>0){
            BigDecimal transferUSDT = balances[0].subtract(balances[1]).divide(new BigDecimal(2),2,RoundingMode.HALF_DOWN);
            if(transferUSDT.compareTo(BigDecimal.ZERO)==0) return;
            BinanceClient.marginRestClient.transfer("USDT",transferUSDT.toString(),TransferType.UMFUTURE_MAIN);
            //synchronize local cache
            while (!MarketCache.futureBalance.compareAndSet(MarketCache.futureBalance.get(),balances[0].subtract(transferUSDT)));
            while (!MarketCache.spotBalance.compareAndSet(MarketCache.spotBalance.get(),balances[1].add(transferUSDT)));
        }else if(balances[1].subtract(balances[0]).compareTo(BigDecimal.ZERO)>0){
            log.info("before do balance origin future balance={}, spot balance={},real future balance={}, spot balance={}"
                    ,MarketCache.futureBalance.get(),MarketCache.spotBalance.get(), balances[0],balances[1]);


            BigDecimal transferUSDT = balances[1].subtract(balances[0]).divide(new BigDecimal(2),2,RoundingMode.HALF_DOWN);
            if(transferUSDT.compareTo(BigDecimal.ZERO)==0) return;
            BinanceClient.marginRestClient.transfer("USDT",transferUSDT.toString(),TransferType.MAIN_UMFUTURE);
            //synchronize local cache
            while (!MarketCache.futureBalance.compareAndSet(MarketCache.futureBalance.get(),balances[0].add(transferUSDT)));
            while (!MarketCache.spotBalance.compareAndSet(MarketCache.spotBalance.get(),balances[1].subtract(transferUSDT)));

        }
        BeanConstant.watchdog =true;
    }

    //get all balance
    @Scheduled(cron = "0 0 2 * * ?")
    public BigDecimal getAllBalance(){
        final BigDecimal[] spotBalance = new BigDecimal[1];
        spotBalance[0] = BigDecimal.ZERO;
        BigDecimal futureBalance = BinanceClient.futureSyncClient.getAccountInformation().getTotalWalletBalance();
        BinanceClient.spotSyncClient.getAccount().getBalances()
                .stream()
                .filter(assetBalance -> new BigDecimal(assetBalance.getFree()).compareTo(BigDecimal.ZERO)>0)
                .forEach(assetBalance -> {
                    String symbol = assetBalance.getAsset()+"USDT";
                    if(MarketCache.spotTickerMap.containsKey(symbol)){
                        BigDecimal askPrice = MarketCache.spotTickerMap.get(symbol).get(BeanConstant.BEST_ASK_PRICE);
                        BigDecimal bidPrice = MarketCache.spotTickerMap.get(symbol).get(BeanConstant.BEST_BID_PRICE);
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

        tagMap.put("name","mengna");
        fileMap.put("balance",spotBalance[0].add(futureBalance));

        influxDbConnection.insert("balance_info",tagMap,fileMap);


        return spotBalance[0].add(futureBalance);
    }

    //buy some bnb for exchange charge, check for 1 in the morning
    @Scheduled(cron = "0 0 1 * * ?")
    public void buyBNB(){
        log.info("buy some bnb for exchange charge");
        AccountInformation accountInformation =  BinanceClient.futureSyncClient.getAccountInformation();
        final BigDecimal[] balances = new BigDecimal[2];
        accountInformation.getAssets().stream().filter(asset -> asset.getAsset().equals("BNB")).forEach(asset -> {
            balances[0] = asset.getMaxWithdrawAmount();
        });

        BinanceClient.spotSyncClient.getAccount().getBalances().stream().filter(assetBalance -> assetBalance.getAsset().equals("BNB"))
                .forEach(assetBalance -> {
                    balances[1] = new BigDecimal(assetBalance.getFree());
                });
        BigDecimal bnbPrice = MarketCache.spotTickerMap.get("BNBUSDT").get(BeanConstant.BEST_ASK_PRICE);
        Integer[] stepSize = tradeUtil.getStepSize("BNBUSDT");

        log.info("future bnb={}, spot bnb={}", balances[0],balances[1]);
        //transfer some bnb to u coin
        if(balances[0].multiply(bnbPrice).compareTo(new BigDecimal(10))<0){
            BinanceClient.marginRestClient.transfer("BNB",balances[1].divide(new BigDecimal(2),4,RoundingMode.HALF_DOWN).toString(),TransferType.MAIN_UMFUTURE);
        }

        // buy some bnb
        if(balances[1].multiply(bnbPrice).compareTo(new BigDecimal(10))<0){
            BigDecimal bnbQty = new BigDecimal("15").divide(bnbPrice,stepSize[1],RoundingMode.HALF_UP);
            NewOrderResponse order = spotSyncClientProxy.newOrder(
                    marketBuy("BNBUSDT",
                            bnbQty.toString()).newOrderRespType(NewOrderResponseType.FULL));
        }
    }

    public static void main(String[] args){
        ConcurrentHashMap<String, Integer> testMap = new ConcurrentHashMap<>();
        for(int i=0;i<10000;i++){
            testMap.put("test"+i,i);
        }
        Iterator<Map.Entry<String, Integer>> iterator= testMap.entrySet().iterator();


        new Runnable(){
            @Override
            public void run() {
//                while (iterator.hasNext()){
//                    Map.Entry<String, Integer> entry = iterator.next();
//                    if(entry.getValue()%2==0){
//                        iterator.remove();
//                    }
//                }

                for(Map.Entry <String, Integer> entry : testMap.entrySet()){
                    if(entry.getValue() % 2 == 0){
                        testMap.remove(entry.getKey());
                    }
                }


            }
        }.run();


        new Runnable(){
            @Override
            public void run() {
                for(int i=10000;i<20000;i++){
                    testMap.put("test"+i,i);
                }
            }
        }.run();

        System.out.println(testMap.size());


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
