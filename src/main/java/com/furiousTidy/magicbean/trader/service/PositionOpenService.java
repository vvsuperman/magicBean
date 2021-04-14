package com.furiousTidy.magicbean.trader.service;

import com.binance.client.model.event.MarkPriceEvent;
import com.binance.client.model.market.ExchangeInfoEntry;
import com.furiousTidy.magicbean.apiproxy.ProxyUtil;
import com.furiousTidy.magicbean.apiproxy.SpotSyncClientProxy;
import com.furiousTidy.magicbean.config.BeanConfig;
import com.furiousTidy.magicbean.dbutil.dao.PairsTradeDao;
import com.furiousTidy.magicbean.dbutil.model.PairsTradeModel;
import com.furiousTidy.magicbean.dbutil.dao.TradeInfoDao;
import com.furiousTidy.magicbean.dbutil.model.TradeInfoModel;
import com.furiousTidy.magicbean.trader.TradeUtil;
import com.furiousTidy.magicbean.util.BeanConstant;
import com.furiousTidy.magicbean.util.MarketCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static com.furiousTidy.magicbean.trader.TradeUtil.getCurrentTime;


/*
*  开仓
*
* */
@Service
public class PositionOpenService {
    static Logger logger = LoggerFactory.getLogger(PositionOpenService.class);

    @Autowired
    SpotSyncClientProxy spotSyncClientProxy;

    @Autowired
    TradeUtil tradeUtil;

    @Autowired
    PairsTradeDao pairsTradeDao;

    @Autowired
    TradeInfoDao tradeInfoDao;

    @Autowired
    TradeService tradeService;

    @Autowired
    ProxyUtil proxyUtil;


    Set<String> closeProcessingSet = new HashSet<>();

    List<PairsTradeModel> pairsTradeList = new ArrayList<>();

    Map<String, TradeInfoModel> tradeInfoMap = new HashMap<>();

    String symbol = "";

    BigDecimal futureBidPrice;

    BigDecimal futureAskPrice;

    BigDecimal spotAskPrice;

    BigDecimal spotBidPrice;

    BigDecimal closeRatio;

    List<PairsTradeModel> symbolPairsTradeList;

    String clientOrderId;



//    //处理资金费率
//    public void processFundingRate(){
//        MarketCache.markPriceList = BinanceClient.futureSyncClient.getMarkPrice(null);
//        Collections.sort( MarketCache.markPriceList, new Comparator<MarkPrice>() {
//            @Override
//            public int compare(MarkPrice o1, MarkPrice o2) {
//                return o2.getLastFundingRate().compareTo(o1.getLastFundingRate());
//            }
//        });
//        for(MarkPrice markPrice : MarketCache.markPriceList){
//            logger.info(markPrice.getLastFundingRate().toString()+":"+markPrice.getSymbol());
//        }
//    }


    //处理list，获取资金费率最高的深度行情
    public void moneyProcess(List<MarkPriceEvent> markPrices){

        //获取当前账户资金


        //资金费率最高的标的
        String symbol = markPrices.get(0).getSymbol();


    }



    /*
    *
    * 实际下单数量，用最小交易单元来交易
    * */
    public void doTradeSymbol(String symbol, BigDecimal totalCost, String direct) throws InterruptedException {
        BigDecimal standardTradeUnit = BeanConfig.STANDARD_TRADE_UNIT;
        while(totalCost.compareTo(standardTradeUnit) > 0){
            doTrade(symbol,standardTradeUnit,direct);
            totalCost = totalCost.subtract(standardTradeUnit);
            Thread.sleep(1000);
        }
        if(totalCost.compareTo(BigDecimal.ZERO) > 0){
            doTrade(symbol,totalCost,direct);
        }
    }

    /*
    * 下单，总价为cost, divide the total cost
    * @Param symbol 标的
    * @Param cost 下单量的usdt
    *
    * */
    public void doTrade(String symbol, BigDecimal cost, String direct) throws InterruptedException {
        logger.info("doTrade start...........");
        BigDecimal futurePrice, spotPrice = BigDecimal.ZERO;

        if(direct.equals(BeanConstant.FUTURE_SELL)){
            do{
                //re-compare the price in the cache
                futurePrice = MarketCache.futureTickerMap.get(symbol).get(BeanConstant.BEST_BID_PRICE);
                spotPrice = MarketCache.spotTickerMap.get(symbol).get(BeanConstant.BEST_ASK_PRICE);
                logger.info("futurePrice："+ futurePrice +" spotprice："+spotPrice+":"+direct);
                Thread.sleep(200);
                //TODO not support pairs now
            } while(futurePrice.subtract(spotPrice).divide(spotPrice,4)
                    .compareTo(BeanConfig.OPEN_PRICE_GAP)<0);
        }else{
            do{
                //re-compare the price in the cache
                futurePrice = MarketCache.futureTickerMap.get(symbol).get(BeanConstant.BEST_ASK_PRICE);
                spotPrice = MarketCache.spotTickerMap.get(symbol).get(BeanConstant.BEST_BID_PRICE);
                logger.info("futurePrice："+ futurePrice +" spotprice："+spotPrice+":"+direct);
                Thread.sleep(200);
                //TODO not support pairs now
            } while(spotPrice.subtract(futurePrice).divide(futurePrice,4)
                    .compareTo(BeanConfig.OPEN_PRICE_GAP)<0);
        }

//        doPairsTrade(symbol, cost, futurePrice, spotPrice,direct);
    }

    //the central control to control the pair trade
    @Async
    public void doPairsTradeRobot() throws InterruptedException {

        for(;;){
            doPairsTradeRobotInner();
            Thread.sleep(BeanConfig.SLEEP_TIME);
        }
    }


    public void doPairsTradeRobotInner() throws InterruptedException {
        if(!BeanConstant.watchdog || BeanConstant.NETWORK_DELAYED) return;

        //if new pairs trade success, then get the pairs trade
        if(BeanConstant.HAS_NEW_TRADE_OPEN.get() || pairsTradeList.size() ==0){
            pairsTradeList.clear();
            pairsTradeList =  pairsTradeDao.getPairsTradeOpen();
            //store trade_info in the map;
            tradeInfoMap.clear();

            pairsTradeList.forEach(pairsTradeModel ->
                    tradeInfoMap.put(pairsTradeModel.getOpenId()
                            ,tradeInfoDao.getTradeInfoByOrderId(pairsTradeModel.getOpenId()))
            );

            //sort the list min to high
            sortPairsTradeList(pairsTradeList);
            BeanConstant.HAS_NEW_TRADE_OPEN.set(false);
        }

        for(Map.Entry<String,ExchangeInfoEntry> entry: MarketCache.futureInfoCache.entrySet()) {

            //compare the price in the cache
            symbol = entry.getKey();
            if(!symbol.contains("USDT")) continue;
            futureBidPrice = getFutureTickPrice(symbol,"bid");
            spotAskPrice= getSpotTickPrice(symbol,"ask");
            if(futureBidPrice == null || spotAskPrice == null ||
                    futureBidPrice.compareTo(BigDecimal.ZERO)==0 || spotAskPrice.compareTo(BigDecimal.ZERO)==0 ) continue;
            //price matched open
            if( BeanConstant.ENOUGH_MONEY.get() && tradeUtil.isTradeCanOpen(symbol)
                    && futureBidPrice.subtract(spotAskPrice).divide(spotAskPrice,4)
                    .compareTo(tradeUtil.getPairsGap(symbol)) > 0){

                clientOrderId = symbol+"_"+BeanConstant.FUTURE_SELL_OPEN+"_"+ getCurrentTime();

                MarketCache.eventLockCache.put(clientOrderId,new ReentrantLock());
                doPairsTrade(symbol, BeanConfig.STANDARD_TRADE_UNIT,futureBidPrice,spotAskPrice,
                        BeanConstant.FUTURE_SELL_OPEN,clientOrderId);

            }else {
                if(!tradeUtil.isTradeCanClosed(symbol)) continue;
                if(pairsTradeList == null || pairsTradeList.size() == 0) continue;
                symbolPairsTradeList = getPairsTradeInList(symbol,pairsTradeList);
                if(symbolPairsTradeList.size() != 0){
                    for(PairsTradeModel pairsTradeModel: symbolPairsTradeList){
                        // if trade already in processing set, then skip the trade
                        if(closeProcessingSet.contains(pairsTradeModel.getOpenId())) continue;

                        futureAskPrice= getFutureTickPrice(symbol,"ask");
                        spotBidPrice = getSpotTickPrice(symbol,"bid");
                        closeRatio = spotBidPrice.subtract(futureAskPrice).divide(futureAskPrice,4,BigDecimal.ROUND_HALF_UP);
                        if( futureAskPrice.compareTo(BigDecimal.ZERO)>0 && spotBidPrice.compareTo(BigDecimal.ZERO)>0
                                && closeRatio.add(pairsTradeModel.getOpenRatio()).compareTo(BeanConfig.CLOSE_PRICE_GAP) > 0){
                            logger.info("begin to close symbol={}, closeRatio={}",symbol,closeRatio);
                            //add trade to processing set
                            closeProcessingSet.add(pairsTradeModel.getOpenId());
                            //begin to close the symbol
                            String clientOrderId = symbol+"_"+BeanConstant.FUTURE_SELL_CLOSE+"_"+ getCurrentTime();
                            //get qty from store
                            String openId = pairsTradeModel.getOpenId();
                            TradeInfoModel tradeInfoModel = tradeInfoMap.containsKey(openId)?tradeInfoMap.get(openId):null;
                            if(tradeInfoModel == null){
                                tradeInfoModel =  tradeInfoDao.getTradeInfoByOrderId(pairsTradeModel.getOpenId());
                            }

                            BigDecimal futrueQty = tradeInfoModel.getFutureQty();
                            BigDecimal spotQty = tradeInfoModel.getSpotQty();
                            pairsTradeModel.setCloseId(clientOrderId);

                            //set the lock
                            Lock closeLock = new ReentrantLock();
                            MarketCache.eventLockCache.put(clientOrderId,new ReentrantLock());
                            closeLock.lock();
                            doPairsTradeByQty(symbol, futrueQty,spotQty,futureAskPrice,spotBidPrice,
                                    BeanConstant.FUTURE_SELL_CLOSE,clientOrderId);
                            //update close id in pairstrade
                            pairsTradeDao.updatePairsTrade(pairsTradeModel);
                            closeLock.unlock();

                        }
                    }
                }
            }
        }
    }

    //do paris trade
    private void doPairsTrade(String symbol, BigDecimal cost, BigDecimal futurePrice, BigDecimal spotPrice,
                              String direct,String clientOrderId) throws InterruptedException {
        //计算合约最小下单位数
        Integer[] stepSize = tradeUtil.getStepSize(symbol);
        //计算合约卖单数量
        BigDecimal futureQuantity =  cost.divide(futurePrice, stepSize[0], BigDecimal.ROUND_HALF_UP);
        //计算现货买单数量
        BigDecimal spotQuantity = cost.divide(spotPrice, stepSize[1], BigDecimal.ROUND_HALF_UP);
        //取位数最大的数量，避免精度问题
        BigDecimal qty = stepSize[0]<stepSize[1]?futureQuantity:spotQuantity;

        doPairsTradeByQty(symbol,qty,qty,futurePrice,spotPrice,direct,clientOrderId);

    }

    //do paris trade
    private void doPairsTradeByQty(String symbol, BigDecimal futureQty, BigDecimal spotQty, BigDecimal futurePrice, BigDecimal spotPrice,
                                   String direct,String clientOrderId) throws InterruptedException {

        if (!checkMoney(futureQty, spotQty, futurePrice, spotPrice, clientOrderId)) return;


        //计算合约最小下单位数
        Integer[] stepSize = tradeUtil.getStepSize(symbol);

        tradeService.doFutureTrade(symbol, futurePrice, futureQty, stepSize[0], direct, clientOrderId);
        tradeService.doSpotTrade(symbol, spotPrice, spotQty, stepSize[1], direct, clientOrderId);
    }

    private boolean checkMoney(BigDecimal futureQty, BigDecimal spotQty, BigDecimal futurePrice, BigDecimal spotPrice, String clientOrderId) {
        // check money enough
        logger.info("check money:clientOrderId={}, futureBalance={}, spotbalance={}",clientOrderId,MarketCache.futureBalance.get(),MarketCache.spotBalance.get());

        if(MarketCache.futureBalance.get().compareTo(BeanConfig.ENOUTH_MOENY_UNIT) <0
                || MarketCache.spotBalance.get().compareTo(BeanConfig.ENOUTH_MOENY_UNIT) <0 ){
            logger.info("not enough for clientid={}, not trade", clientOrderId);
            return false;
        }

        //money enough, set balance
        proxyUtil.addBalance(futurePrice.multiply(futureQty).negate(),"future");
        proxyUtil.addBalance( spotPrice.multiply(spotQty).negate(),"spot");
        return true;
    }

    private BigDecimal getFutureTickPrice(String symbol,String type) {
        if(MarketCache.futureTickerMap.containsKey(symbol)){
            if(type.equals("bid")){
                return  MarketCache.futureTickerMap.get(symbol).get(BeanConstant.BEST_BID_PRICE);
            }else {
                return  MarketCache.futureTickerMap.get(symbol).get(BeanConstant.BEST_ASK_PRICE);
            }
        }else{
            return null;
        }
    }

    private BigDecimal getSpotTickPrice(String symbol,String type) {
        if(MarketCache.spotTickerMap.containsKey(symbol)){
            if(type.equals("bid")){
                return  MarketCache.spotTickerMap.get(symbol).get(BeanConstant.BEST_BID_PRICE);
            }else{
                return  MarketCache.spotTickerMap.get(symbol).get(BeanConstant.BEST_ASK_PRICE);

            }
        }else{
            return null;
        }
    }

    private void sortPairsTradeList(List<PairsTradeModel> pairsTradeList) {
        if(pairsTradeList!=null && pairsTradeList.size()>0){
            Collections.sort(pairsTradeList,
                    (Comparator<PairsTradeModel>) (a, b) -> {
                        if(a.getOpenRatio()!=null && b.getOpenRatio()!=null){
                          return  a.getOpenRatio().compareTo(b.getOpenRatio());
                        }else{
                            return 0;
                        }
                    }
            );
        }
    }

    //get symbol's all the pairs info
    public List<PairsTradeModel> getPairsTradeInList(String symbol, List<PairsTradeModel> pairsTradeList){
        List<PairsTradeModel> symbolPairsTradeList = new ArrayList<>();
        for(PairsTradeModel pairsTradeModel: pairsTradeList){
            if(pairsTradeModel.getSymbol().equals(symbol)){
                if(pairsTradeModel.getOpenRatio() == null) continue;
                symbolPairsTradeList.add(pairsTradeModel);
            }
        }
        return symbolPairsTradeList;
    }






    public static void main(String[] args) {
//       String value = "0.0001";
//        MathContext mathContext = new MathContext(2,RoundingMode.HALF_UP);
//        System.out.println(new BigDecimal(value));
        LocalDate today = LocalDate.now();

        LocalTime time = LocalTime.now();
        System.out.println(today.getYear()+"/"+today.getMonthValue()+"/"+today.getDayOfMonth()+" "+time);

    }
}
