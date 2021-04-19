package com.furiousTidy.magicbean.trader.service;

import com.binance.client.model.trade.Order;
import com.furiousTidy.magicbean.dbutil.dao.PairsTradeDao;
import com.furiousTidy.magicbean.dbutil.dao.TradeInfoDao;
import com.furiousTidy.magicbean.dbutil.model.PairsTradeModel;
import com.furiousTidy.magicbean.dbutil.model.TradeInfoModel;
import com.furiousTidy.magicbean.trader.TradeUtil;
import com.furiousTidy.magicbean.util.BeanConstant;
import com.furiousTidy.magicbean.util.MarketCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


@Service
@Slf4j
public class AfterOrderService {

    @Autowired
    TradeInfoDao tradeInfoDao;

    @Autowired
    PairsTradeDao pairsTradeDao;

    @Autowired
    TradeUtil tradeUtil;

    @Async
    public void processFutureOrder(String clientOrderId, Order order){
        log.info(" future store begin,symbol={}, price={},qty={}",order.getSymbol(),order.getPrice(),order.getAvgPrice());

        Lock eventLock = null;
        try {
            //we need a lock to lock pairs trade, or insert may exception
             eventLock = MarketCache.eventLockCache.containsKey(clientOrderId)?MarketCache.eventLockCache.get(clientOrderId):null;
            if(eventLock == null){
                eventLock = new ReentrantLock();
                MarketCache.eventLockCache.put(clientOrderId, eventLock);
            }
            eventLock.lock();
            BigDecimal price = order.getAvgPrice();
            BigDecimal qty = order.getExecutedQty();
            TradeInfoModel tradeInfo =  tradeInfoDao.getTradeInfoByOrderId(clientOrderId);
            if(tradeInfo == null){
                tradeInfo = new TradeInfoModel();
                tradeInfo.setSymbol(order.getSymbol());
                tradeInfo.setOrderId(clientOrderId);
                tradeInfo.setFuturePrice(price);
                tradeInfo.setFutureQty(qty);
                tradeInfoDao.insertTradeInfo(tradeInfo);
            }
            else{
                BigDecimal futurePrice, futureQty;
                int priceSize = order.getPrice().toString().length() - order.getPrice().toString().indexOf(".");
                //calculate bid price
                if(tradeInfo.getFuturePrice() == null){
                    futurePrice = price;
                    futureQty = qty;
                }else{
                    futureQty = qty.add(tradeInfo.getFutureQty());
                    futurePrice = price.multiply(qty)
                            .add(tradeInfo.getFuturePrice().multiply(tradeInfo.getFutureQty()))
                            .divide(futureQty,priceSize);
                }

                tradeInfo.setFutureQty(futureQty);
                tradeInfo.setFuturePrice(futurePrice);
                tradeInfoDao.updateTradeInfoById(tradeInfo);

                log.info("future calculate ratio begin, tradeInfo={}", tradeInfo);

                //calcualte ratio
                if (tradeInfo.getSpotPrice() != null) {
                    BigDecimal spotPrice = tradeInfo.getSpotPrice();
                    calculateRatioAndProfit(order.getSymbol(),clientOrderId,futurePrice,spotPrice,priceSize);
                }
            }

        } catch (Exception e) {
            log.error("future process......failed {}",e);
        }finally {
            eventLock.unlock();
        }
    }

    @Async
    public void processSpotOrder(String symbol, String clientOrderId, BigDecimal price, BigDecimal qty){
        log.info(" spot store begin,symbol={}, price={},qty={}",symbol,price,qty);
        //we need a lock to lock pairs trade, or insert may exception
        Lock eventLock = MarketCache.eventLockCache.containsKey(clientOrderId)?MarketCache.eventLockCache.get(clientOrderId):null;
        if(eventLock == null){
            eventLock = new ReentrantLock();
            MarketCache.eventLockCache.put(clientOrderId, eventLock);
        }

        try {
            eventLock.lock();
            TradeInfoModel tradeInfo = tradeInfoDao.getTradeInfoByOrderId(clientOrderId);

            if (tradeInfo == null) {
                tradeInfo = new TradeInfoModel();
                tradeInfo.setSymbol(symbol);
                tradeInfo.setOrderId(clientOrderId);
                tradeInfo.setSpotPrice(price);
                tradeInfo.setSpotQty(qty);
                log.info("spot store process insert, tradeInfo {}",tradeInfo);

                tradeInfoDao.insertTradeInfo(tradeInfo);
            } else {
                BigDecimal spotPrice, spotQty;
                BigDecimal ratio;
                log.info("spot store process info, tradeInfo {}",tradeInfo);
                int priceSize = price.toString().length() - price.toString().indexOf(".");
                //calculate bid price
                if (tradeInfo.getSpotPrice() == null) {
                    spotPrice = price;
                    spotQty = qty;
                } else {
                    spotQty = qty.add(tradeInfo.getSpotQty());
                    spotPrice = price.multiply(qty)
                            .add(tradeInfo.getSpotPrice().multiply(tradeInfo.getSpotQty()))
                            .divide(spotQty, priceSize, RoundingMode.HALF_UP);
                    log.info("spot store process calculate, tradeInfo {}",spotQty,spotPrice);

                }
                tradeInfo.setSpotQty(spotQty);
                tradeInfo.setSpotPrice(spotPrice);
                log.info("spot store process update, tradeInfo {}",tradeInfo);
                tradeInfoDao.updateTradeInfoById(tradeInfo);

                //calcualte ratio
                log.info("spot calculate ratio begin, tradeInfo={}", tradeInfo);
                if (tradeInfo.getFuturePrice() != null) {
                    BigDecimal futurePrice = tradeInfo.getFuturePrice();
                    calculateRatioAndProfit(symbol,clientOrderId, futurePrice, spotPrice, priceSize);
                }

            }
        } catch (Exception e) {
            log.error("spot process......failed {}",e);
        }finally {
            eventLock.unlock();
        }
    }

    private void calculateRatioAndProfit(String symbol, String clientOrderId, BigDecimal futurePrice, BigDecimal spotPrice, int priceSize) {
        BigDecimal ratio;
        BigDecimal profit;
        log.info("in calucalateRatio, symbol={}, clientorderid={}, futureprice={}, spotprice={}",symbol,clientOrderId,futurePrice,spotPrice);
        if (clientOrderId.contains(BeanConstant.FUTURE_SELL_OPEN)) {
            ratio = futurePrice.subtract(spotPrice).divide(spotPrice, 6,RoundingMode.HALF_UP);
            PairsTradeModel pairsTradeModel =pairsTradeDao.getPairsTradeByOpenId(clientOrderId);
            if(pairsTradeModel == null) {
                pairsTradeModel = new PairsTradeModel();
                pairsTradeModel.setSymbol(symbol);
                pairsTradeModel.setOpenId(clientOrderId);
                pairsTradeModel.setOpenRatio(ratio);
                log.info("insert pairstrade,pairsTrade={}",pairsTradeModel);
                pairsTradeDao.insertPairsTrade(pairsTradeModel);
            }else{
                log.info("update pairstrade,pairsTrade={}",pairsTradeModel);
                pairsTradeDao.updateOpenRatioByOpenId(clientOrderId, ratio);
            }

        } else if (clientOrderId.contains(BeanConstant.FUTURE_SELL_CLOSE)) {

            PairsTradeModel pairsTradeModel = pairsTradeDao.getPairsTradeByCloseId(clientOrderId);
            TradeInfoModel tradeInfoModel = tradeInfoDao.getTradeInfoByOrderId(pairsTradeModel.getOpenId());
            profit = tradeUtil.getProfit(tradeInfoModel.getFuturePrice(),tradeInfoModel.getSpotPrice(),futurePrice,spotPrice,tradeInfoModel.getFutureQty());
            ratio = spotPrice.subtract(futurePrice).divide(futurePrice, 6, RoundingMode.HALF_UP);
            pairsTradeModel.setProfit(profit);
            pairsTradeModel.setCloseRatio(ratio);
            pairsTradeDao.updatePairsTrade(pairsTradeModel);
        }

//        tradeUtil.checkUSDEnough();
    }


}
