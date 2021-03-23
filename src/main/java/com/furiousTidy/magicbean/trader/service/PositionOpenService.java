package com.furiousTidy.magicbean.trader.service;

import com.binance.client.model.event.MarkPriceEvent;
import com.binance.client.model.market.ExchangeInfoEntry;
import com.binance.client.model.market.MarkPrice;
import com.furiousTidy.magicbean.apiproxy.SpotSyncClientProxy;
import com.furiousTidy.magicbean.config.BeanConfig;
import com.furiousTidy.magicbean.dbutil.dao.PairsTradeDao;
import com.furiousTidy.magicbean.dbutil.model.PairsTradeModel;
import com.furiousTidy.magicbean.dbutil.dao.TradeInfoDao;
import com.furiousTidy.magicbean.dbutil.model.TradeInfoModel;
import com.furiousTidy.magicbean.trader.TradeUtil;
import com.furiousTidy.magicbean.trader.controller.PositionOpenController;
import com.furiousTidy.magicbean.util.BeanConstant;
import com.furiousTidy.magicbean.util.BinanceClient;
import com.furiousTidy.magicbean.util.MarketCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
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

    //处理资金费率
    public void processFundingRate(){
        MarketCache.markPriceList = BinanceClient.futureSyncClient.getMarkPrice(null);
        Collections.sort( MarketCache.markPriceList, new Comparator<MarkPrice>() {
            @Override
            public int compare(MarkPrice o1, MarkPrice o2) {
                return o2.getLastFundingRate().compareTo(o1.getLastFundingRate());
            }
        });
        for(MarkPrice markPrice : MarketCache.markPriceList){
            logger.info(markPrice.getLastFundingRate().toString()+":"+markPrice.getSymbol());
        }
    }


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
    public void doPairsTradeRobot() throws InterruptedException {
        String symbol = "";
        while(PositionOpenController.watchdog){
            //select futureBid from futureBid where symbol = symbol;
            List<PairsTradeModel> pairsTradeList =  pairsTradeDao.getPairsTradeOpen();
            //sort the mim one
            sortPairsTradeList(pairsTradeList);

            for(Map.Entry<String,ExchangeInfoEntry> entry: MarketCache.futureInfoCache.entrySet()) {

                    //re-compare the price in the cache
                    symbol = entry.getKey();
                    if(!symbol.contains("USDT")) continue;
                    BigDecimal futureBidPrice = getFutureTickPrice(symbol,"bid");
                    BigDecimal spotAskPrice = getSpotTickPrice(symbol,"ask");
                    if(futureBidPrice == null || spotAskPrice == null ||
                            futureBidPrice.compareTo(BigDecimal.ZERO)==0 || spotAskPrice.compareTo(BigDecimal.ZERO)==0 ) continue;
                    //price matched open
                    if(tradeUtil.isUSDTenough()
                            &&  futureBidPrice.subtract(spotAskPrice).divide(spotAskPrice,4).compareTo(BeanConfig.OPEN_PRICE_GAP) > 0){

                        String clientOrderId = symbol+"_"+BeanConstant.FUTURE_SELL_OPEN+"_"+ getCurrentTime();

                        MarketCache.eventLockCache.put(clientOrderId,new ReentrantLock());
                        doPairsTrade(symbol, BeanConfig.STANDARD_TRADE_UNIT,futureBidPrice,spotAskPrice,
                                BeanConstant.FUTURE_SELL_OPEN,clientOrderId);

//                        PositionOpenController.watchdog = false;
//                        break;
                    }else {
                        if(pairsTradeList == null || pairsTradeList.size() == 0) continue;
                        List<PairsTradeModel> symbolPairsTradeList = getPairsTradeInList(symbol,pairsTradeList);
                        if(symbolPairsTradeList.size() != 0){
                            for(PairsTradeModel pairsTradeModel: symbolPairsTradeList){
                                BigDecimal futureAskPrice = getFutureTickPrice(symbol,"ask");
                                BigDecimal spotBidPrice = getSpotTickPrice(symbol,"bid");
                                if( futureAskPrice.compareTo(BigDecimal.ZERO)>0 && spotBidPrice.compareTo(BigDecimal.ZERO)>0
                                        && spotBidPrice.subtract(futureAskPrice).divide(futureAskPrice,4,BigDecimal.ROUND_HALF_UP)
                                        .add(pairsTradeModel.getOpenRatio()).compareTo(BeanConfig.CLOSE_PRICE_GAP) > 0){

                                    //begin to close the symbol
                                    String clientOrderId = symbol+"_"+BeanConstant.FUTURE_SELL_CLOSE+"_"+ getCurrentTime();

                                    TradeInfoModel tradeInfoModel = tradeInfoDao.getTradeInfoByOrderId(pairsTradeModel.getOpenId());
                                    BigDecimal qty = tradeInfoModel.getFutureQty();

                                    pairsTradeModel.setCloseId(clientOrderId);
                                    //update close id in pairstrade
                                    pairsTradeDao.updatePairsTrade(pairsTradeModel);

                                    doPairsTradeByQty(symbol, qty,futureAskPrice,spotBidPrice,
                                            BeanConstant.FUTURE_SELL_CLOSE,clientOrderId);

//                                    PositionOpenController.watchdog = false;
//                                    break;
                                }
                            }
                        }
                    }
            }
            Thread.sleep(200);
        }
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

    //do paris trade
    private void doPairsTrade(String symbol, BigDecimal cost, BigDecimal futurePrice, BigDecimal spotPrice,
                              String direct,String clientOrderId) throws InterruptedException {
        //计算合约最小下单位数
        Integer[] stepSize = tradeUtil.getStepSize(symbol);
        //计算合约卖单数量
        BigDecimal futureQuantity =  cost.divide(futurePrice, stepSize[0], BigDecimal.ROUND_HALF_UP);
        //计算现货买单数量
        BigDecimal spotQuantity = cost.divide(spotPrice, stepSize[1], BigDecimal.ROUND_HALF_UP);
        logger.info("trade future qty:{} spot qty:{}",futureQuantity,spotQuantity);
        //do the order
        BigDecimal qty = futurePrice.compareTo(spotPrice)>0?spotQuantity:futureQuantity;
        int qtyStepSize = stepSize[0]<stepSize[1]?stepSize[0]:stepSize[1];
        qty = qty.setScale(qtyStepSize);
        logger.info("actual  qty:{}",qty);


        tradeService.doFutureTrade(symbol, futurePrice, qty, stepSize[0], direct, clientOrderId);
        tradeService.doSpotTrade(symbol, spotPrice, qty, stepSize[1], direct, clientOrderId);
    }

    //do paris trade
    private void doPairsTradeByQty(String symbol, BigDecimal qty, BigDecimal futurePrice, BigDecimal spotPrice,
                              String direct,String clientOrderId) throws InterruptedException {
        //计算合约最小下单位数
        Integer[] stepSize = tradeUtil.getStepSize(symbol);

        tradeService.doFutureTrade(symbol, futurePrice, qty, stepSize[0], direct, clientOrderId);
        tradeService.doSpotTrade(symbol, spotPrice, qty, stepSize[1], direct, clientOrderId);
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