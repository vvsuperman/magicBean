package com.furiousTidy.magicbean.trader.service;

import com.binance.client.model.event.SymbolBookTickerEvent;
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
import com.furiousTidy.magicbean.util.BookTickerModel;
import com.furiousTidy.magicbean.util.MarketCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
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


//    List<PairsTradeModel> pairsTradeList = new ArrayList<>();
//
//    Map<String, TradeInfoModel> tradeInfoMap = new HashMap<>();

    String symbol = "";

    BigDecimal futureBidPrice;

    BigDecimal futureAskPrice;

    BigDecimal spotAskPrice;

    BigDecimal spotBidPrice;

    BigDecimal closeRatio;

    List<PairsTradeModel> symbolPairsTradeList;

    String clientOrderId;

    BigDecimal profit;

    static int counter=0;

    BigDecimal origOpenRatio;

    /*
    *
    * 实际下单数量，用最小交易单元来交易
    * */
//    public void doTradeSymbol(String symbol, BigDecimal totalCost, String direct) throws InterruptedException {
//        BigDecimal standardTradeUnit = BeanConfig.STANDARD_TRADE_UNIT;
//        while(totalCost.compareTo(standardTradeUnit) > 0){
//            doTrade(symbol,standardTradeUnit,direct);
//            totalCost = totalCost.subtract(standardTradeUnit);
//            Thread.sleep(1000);
//        }
//        if(totalCost.compareTo(BigDecimal.ZERO) > 0){
//            doTrade(symbol,totalCost,direct);
//        }
//    }

    /*
    * 下单，总价为cost, divide the total cost
    * @Param symbol 标的
    * @Param cost 下单量的usdt
    *
    * */
//    public void doTrade(String symbol, BigDecimal cost, String direct) throws InterruptedException {
//        logger.info("doTrade start...........");
//        BigDecimal futurePrice, spotPrice = BigDecimal.ZERO;
//
//        if(direct.equals(BeanConstant.FUTURE_SELL)){
//            do{
//                //re-compare the price in the cache
//                futurePrice = MarketCache.futureTickerMap.get(symbol).get(BeanConstant.BEST_BID_PRICE);
//                spotPrice = MarketCache.spotTickerMap.get(symbol).get(BeanConstant.BEST_ASK_PRICE);
//                logger.info("futurePrice："+ futurePrice +" spotprice："+spotPrice+":"+direct);
//                Thread.sleep(200);
//                //TODO not support pairs now
//            } while(futurePrice.subtract(spotPrice).divide(spotPrice,4)
//                    .compareTo(BeanConfig.OPEN_PRICE_GAP)<0);
//        }else{
//            do{
//                //re-compare the price in the cache
//                futurePrice = MarketCache.futureTickerMap.get(symbol).get(BeanConstant.BEST_ASK_PRICE);
//                spotPrice = MarketCache.spotTickerMap.get(symbol).get(BeanConstant.BEST_BID_PRICE);
//                logger.info("futurePrice："+ futurePrice +" spotprice："+spotPrice+":"+direct);
//                Thread.sleep(200);
//                //TODO not support pairs now
//            } while(spotPrice.subtract(futurePrice).divide(futurePrice,4)
//                    .compareTo(BeanConfig.OPEN_PRICE_GAP)<0);
//        }
//
////        doPairsTrade(symbol, cost, futurePrice, spotPrice,direct);
//    }

    //the central control to control the pair trade
    @Async
    public void doPairsTradeRobot() throws InterruptedException {

        for(;;){
            doPairsTradeRobotInner();
            Thread.sleep(BeanConfig.SLEEP_TIME);
        }
    }


    public void doPairsTradeRobotInner() throws InterruptedException {
        if(!BeanConstant.watchdog) return;

        counter++;
        if(counter+1 == 300000) {
            counter = 1;
            BeanConstant.closeImpactSet.clear();
            BeanConstant.openImpactSet.clear();
        }

        for(Map.Entry<String,ExchangeInfoEntry> entry: MarketCache.futureInfoCache.entrySet()) {

            //compare the price in the cache
            symbol = entry.getKey();
            if(!symbol.contains("USDT")) continue;
//            futureBidPrice = getFutureTickPrice(symbol,"bid");
//            futureAskPrice = getFutureTickPrice(symbol,"ask");
            SymbolBookTickerEvent symbolBookTickerEvent = MarketCache.futureTickerMap.get(symbol);
            BookTickerModel bookTickerModel = MarketCache.spotTickerMap.get(symbol);
            if(symbolBookTickerEvent!=null && bookTickerModel!=null){
                symbolBookTickerEvent.setFutureTickDelayTime(System.currentTimeMillis() - symbolBookTickerEvent.getTradeTime());
                bookTickerModel.setSpotTickDelayTime(System.currentTimeMillis() - bookTickerModel.getTradeTime());
                processPairsTrade(symbol,symbolBookTickerEvent,bookTickerModel);
            }

        }
    }

    @Async
    public void processPairsTrade(String symbol,SymbolBookTickerEvent symbolBookTickerEvent,
                                   BookTickerModel bookTickerModel) throws InterruptedException {

        if(symbolBookTickerEvent == null || bookTickerModel == null ) return;

        origOpenRatio = symbolBookTickerEvent.getBestBidPrice()
                .subtract(bookTickerModel.getAskPrice()).divide(bookTickerModel.getAskPrice(),4);

//            if(openRatio.compareTo(tradeUtil.getPairsGap(symbol)) > 0){
//                BeanConstant.openImpactSet.add(symbol+counter);
//            }

        countRatio(origOpenRatio);
        //price matched open
        if(  tradeUtil.isTradeCanOpen(symbol)
//                    && checkImpactSet(symbol,counter, BeanConstant.openImpactSet, BeanConfig.OPEN_IMPACT_COUNTER)
                && origOpenRatio.compareTo(tradeUtil.getPairsGap(symbol)) > 0
                && checkMoney()
                && checkDelay(symbolBookTickerEvent,bookTickerModel)
                ){

            logger.info("check open ratio success, symbol={}, openRatio={}", symbol, origOpenRatio);

            clientOrderId = symbol+"_"+BeanConstant.FUTURE_SELL_OPEN+"_"+ getCurrentTime();

            MarketCache.eventLockCache.put(clientOrderId,new ReentrantLock());
            doPairsTrade(symbol, BeanConfig.STANDARD_TRADE_UNIT,symbolBookTickerEvent,bookTickerModel,
                    BeanConstant.FUTURE_SELL_OPEN,clientOrderId,origOpenRatio);

        }else {
            if(!tradeUtil.isTradeCanClosed(symbol)
                    || ! checkDelay(symbolBookTickerEvent,bookTickerModel)) return;
            if(BeanConstant.pairsTradeList == null || BeanConstant.pairsTradeList.size() == 0) return;
            symbolPairsTradeList = getPairsTradeInList(symbol,BeanConstant.pairsTradeList);
            if(symbolPairsTradeList.size() != 0){
                for(PairsTradeModel pairsTradeModel: symbolPairsTradeList){
                    // if trade already in processing set, then skip the trade
                    String clientOrderId = symbol+"_"+BeanConstant.FUTURE_SELL_CLOSE+"_"+ getCurrentTime();

                    if(BeanConstant.closeProcessingSet.contains(pairsTradeModel.getOpenId())) continue;

//                  closeRatio = spotBidPrice.subtract(futureAskPrice).divide(futureAskPrice,4,BigDecimal.ROUND_HALF_UP);

                    //get tradeinfo from cache or db
                    String openId = pairsTradeModel.getOpenId();
                    TradeInfoModel tradeInfoModel = BeanConstant.tradeInfoMap.containsKey(openId)? BeanConstant.tradeInfoMap.get(openId):null;
                    if(tradeInfoModel == null){
                        tradeInfoModel =  tradeInfoDao.getTradeInfoByOrderId(pairsTradeModel.getOpenId());
                        logger.info("get  model from DB is null ,opnenId={}", openId);
                        if(tradeInfoModel == null) return;
                    }
                     symbolBookTickerEvent = MarketCache.futureTickerMap.get(symbol);
                     bookTickerModel = MarketCache.spotTickerMap.get(symbol);
                    if(symbolBookTickerEvent==null || bookTickerModel == null) return;

                    symbolBookTickerEvent.setFutureTickDelayTime(System.currentTimeMillis() - symbolBookTickerEvent.getTradeTime());
                    bookTickerModel.setSpotTickDelayTime(System.currentTimeMillis() - bookTickerModel.getTradeTime());

                    futureAskPrice = symbolBookTickerEvent.getBestAskPrice();
                    spotBidPrice = bookTickerModel.getBidPrice();
                    //get qty form trade info
                    BigDecimal spotQty = tradeInfoModel.getSpotQty();
                    BigDecimal futrueQty = tradeInfoModel.getFutureQty();
                    //calculate profit
                    profit = tradeUtil.getProfit(tradeInfoModel.getFuturePrice(), futureAskPrice
                            , tradeInfoModel.getSpotPrice(),spotBidPrice,spotQty);


//                    if(profit.compareTo(BeanConfig.TRADE_PROFIT) > 0){
//                        BeanConstant.closeImpactSet.add(symbol+counter);
//                    }else{
//                        continue;
//                    }

                    if( futureAskPrice.compareTo(BigDecimal.ZERO)>0 && spotBidPrice.compareTo(BigDecimal.ZERO)>0
//                            && checkImpactSet(symbol,counter, BeanConstant.closeImpactSet, BeanConfig.CLOSE_IMPACT_COUNTER)
                            && profit.compareTo(BeanConfig.TRADE_PROFIT) > 0
                            && checkDelay(symbolBookTickerEvent,bookTickerModel)
                            ){

                        logger.info("check profit success, symbol={},counter={}, set={}", symbol, counter, BeanConstant.closeImpactSet);
                        closeRatio = spotBidPrice.subtract(futureAskPrice).divide(futureAskPrice, 6, RoundingMode.HALF_UP);
                        //add trade to processing set
                        BeanConstant.closeProcessingSet.add(pairsTradeModel.getOpenId());
                        //begin to close the symbol
                        logger.info("begin to close symbol={},openId={}, closeRatio={}",symbol,clientOrderId,profit);

                        pairsTradeModel.setCloseId(clientOrderId);

                        //set the lock
                        Lock closeLock = new ReentrantLock();
                        MarketCache.eventLockCache.put(clientOrderId,new ReentrantLock());
                        closeLock.lock();
                        doPairsTradeByQty(symbol, futrueQty,spotQty,symbolBookTickerEvent,bookTickerModel,
                                BeanConstant.FUTURE_SELL_CLOSE,clientOrderId,closeRatio);
                        //update close id in pairstrade
                        pairsTradeDao.updatePairsTrade(pairsTradeModel);
                        closeLock.unlock();

                    }
                }
            }
        }
    }

    private boolean checkDelay(SymbolBookTickerEvent symbolBookTickerEvent, BookTickerModel bookTickerModel) {
            if(symbolBookTickerEvent.getFutureTickDelayTime() < 20 && bookTickerModel.getSpotTickDelayTime() <20 ){
                return true;
            }
            return false;

    }

    private void countRatio(BigDecimal openRatio) {

        if(openRatio.compareTo(BeanConstant.b10)>0){
            BeanConstant.BigThanB10++;
        }else if(openRatio.compareTo(BeanConstant.b9)>0){
            BeanConstant.BigThanB9++;
        }else if(openRatio.compareTo(BeanConstant.b8)>0){
            BeanConstant.BigThanB8++;
        }else if(openRatio.compareTo(BeanConstant.b7)>0){
            BeanConstant.BigThanB7++;
        }else if(openRatio.compareTo(BeanConstant.b6)>0){
            BeanConstant.BigThanB6++;
        }else if(openRatio.compareTo(BeanConstant.b5)>0){
            BeanConstant.BigThanB5++;
        }else if(openRatio.compareTo(BeanConstant.b4)>0){
            BeanConstant.BigThanB4++;
        }
    }

    //do paris trade
    private void doPairsTrade(String symbol, BigDecimal cost, SymbolBookTickerEvent symbolBookTickerEvent, BookTickerModel bookTickerModel,
                              String direct,String clientOrderId,BigDecimal ratio) throws InterruptedException {
        //计算合约最小下单位数
        Integer[] stepSize = tradeUtil.getStepSize(symbol);
        //计算合约卖单数量
        BigDecimal futureQuantity =  cost.divide(symbolBookTickerEvent.getBestBidPrice(), stepSize[0], BigDecimal.ROUND_HALF_UP);
        //计算现货买单数量
        BigDecimal spotQuantity = cost.divide(bookTickerModel.getAskPrice(), stepSize[1], BigDecimal.ROUND_HALF_UP);
        //取位数最大的数量，避免精度问题
        BigDecimal qty = stepSize[0]<stepSize[1]?futureQuantity:spotQuantity;

        doPairsTradeByQty(symbol,qty,qty,symbolBookTickerEvent,bookTickerModel,direct,clientOrderId,ratio);

    }

    //do paris trade
    private void doPairsTradeByQty(String symbol, BigDecimal futureQty, BigDecimal spotQty,SymbolBookTickerEvent symbolBookTickerEvent, BookTickerModel bookTickerModel,
                                   String direct,String clientOrderId, BigDecimal ratio) throws InterruptedException {



        //计算合约最小下单位数
        Integer[] stepSize = tradeUtil.getStepSize(symbol);

        tradeService.doFutureTrade(symbol, symbolBookTickerEvent, futureQty, stepSize[0], direct, clientOrderId,ratio);
        tradeService.doSpotTrade(symbol, bookTickerModel, spotQty, stepSize[1], direct, clientOrderId,ratio);
    }

    private boolean checkImpactSet(String symbol, int counter, Set<String> impactSet,int n ){

        for(int i = 0; i < n; i++){
            if(!impactSet.contains(symbol + (counter-i))){
                return false;
            }
        }
        return true;
    }

    // check money enough
    private boolean checkMoney() {
//        logger.info("checkMoney for id={}, futureBalance={}, spotBalance={}", clientOrderId, MarketCache.futureBalance.get(),MarketCache.spotBalance.get());

        if(MarketCache.futureBalance.get().compareTo(BeanConfig.ENOUTH_MOENY_UNIT) <0
                || MarketCache.spotBalance.get().compareTo(BeanConfig.ENOUTH_MOENY_UNIT) <0
                || !BeanConstant.ENOUGH_MONEY.get()){
            return false;
        }

        //money enough, set balance
        proxyUtil.addBalance(BeanConfig.STANDARD_TRADE_UNIT.negate(),"future");
        proxyUtil.addBalance(BeanConfig.STANDARD_TRADE_UNIT.negate(),"spot");

        return true;

    }

    private BigDecimal getFutureTickPrice(String symbol,String type) {
        if(MarketCache.futureTickerMap.containsKey(symbol)){
            if(type.equals("bid")){
//                return  MarketCache.futureTickerMap.get(symbol).get(BeanConstant.BEST_BID_PRICE);
                return MarketCache.futureTickerMap.get(symbol).getBestBidPrice();
            }else {
//                return  MarketCache.futureTickerMap.get(symbol).get(BeanConstant.BEST_ASK_PRICE);
                return  MarketCache.futureTickerMap.get(symbol).getBestAskPrice();
            }
        }else{
            return null;
        }
    }

    private BigDecimal getSpotTickPrice(String symbol,String type) {
        if(MarketCache.spotTickerMap.containsKey(symbol)){
            if(type.equals("bid")){
                return  MarketCache.spotTickerMap.get(symbol).getBidPrice();
            }else{
                return  MarketCache.spotTickerMap.get(symbol).getAskPrice();

            }
        }else{
            return null;
        }
    }

    public void sortPairsTradeList(List<PairsTradeModel> pairsTradeList) {
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

        BeanConstant.closeImpactSet.add("test1");
        System.out.println(BeanConstant.closeImpactSet.remove("abc"));

    }
}
