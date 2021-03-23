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

import static java.util.concurrent.TimeUnit.MILLISECONDS;


@Service
@Slf4j
public class OrderStoreService {

    @Autowired
    TradeInfoDao tradeInfoDao;

    @Autowired
    PairsTradeDao pairsTradeDao;

    @Async
    public void processFutureOrder(String clientOrderId, Order order){
        Lock eventLock = null;
        try {
            //we need a lock to lock pairs trade, or insert may exception
             eventLock = MarketCache.eventLockCache.containsKey(clientOrderId)?MarketCache.eventLockCache.get(clientOrderId):null;
            if(eventLock == null){
                eventLock = new ReentrantLock();
                MarketCache.eventLockCache.put(clientOrderId, eventLock);
            }
            if(eventLock.tryLock(2000,MILLISECONDS)){
                BigDecimal price = order.getPrice();
                BigDecimal qty = order.getExecutedQty();
                TradeInfoModel tradeInfo =  tradeInfoDao.getTradeInfoByOrderId(clientOrderId);
                if(tradeInfo == null){
                    tradeInfo = new TradeInfoModel();
                    tradeInfo.setSymbol(order.getSymbol());
                    tradeInfo.setOrderId(clientOrderId);
                    tradeInfo.setFuturePrice(price);
                    tradeInfo.setFutureQty(qty);
                    tradeInfo.setCreateTime(TradeUtil.getCurrentTime());
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
                    //calcualte ratio
                    if (tradeInfo.getSpotPrice() != null) {
                        BigDecimal spotPrice = tradeInfo.getSpotPrice();
                        calculateRatio(order.getSymbol(),clientOrderId,futurePrice,spotPrice,priceSize);
                    }

                    tradeInfo.setFutureQty(futureQty);
                    tradeInfo.setFuturePrice(futurePrice);

                    tradeInfoDao.updateTradeInfoById(tradeInfo);
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

        //we need a lock to lock pairs trade, or insert may exception
        Lock eventLock = MarketCache.eventLockCache.containsKey(clientOrderId)?MarketCache.eventLockCache.get(clientOrderId):null;
        if(eventLock == null){
            eventLock = new ReentrantLock();
            MarketCache.eventLockCache.put(clientOrderId, eventLock);
        }

        try {
            if(eventLock.tryLock(2000,MILLISECONDS)) {
                TradeInfoModel tradeInfo = tradeInfoDao.getTradeInfoByOrderId(clientOrderId);

                if (tradeInfo == null) {
                    tradeInfo = new TradeInfoModel();
                    tradeInfo.setSymbol(symbol);
                    tradeInfo.setOrderId(clientOrderId);
                    tradeInfo.setSpotPrice(price);
                    tradeInfo.setSpotQty(qty);
                    tradeInfo.setCreateTime(TradeUtil.getCurrentTime());
                    tradeInfoDao.insertTradeInfo(tradeInfo);
                } else {
                    BigDecimal spotPrice, spotQty;
                    BigDecimal ratio;

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
                    }
                    //calcualte ratio
                    if (tradeInfo.getFuturePrice() != null) {
                        BigDecimal futurePrice = tradeInfo.getFuturePrice();
                        calculateRatio(symbol,clientOrderId, futurePrice, spotPrice, priceSize);
                    }

                    tradeInfo.setSpotQty(spotQty);
                    tradeInfo.setSpotPrice(spotPrice);

                    tradeInfoDao.updateTradeInfoById(tradeInfo);

                }
            }
        } catch (Exception e) {
            log.error("spot process......failed {}",e);
        }finally {
            eventLock.unlock();
        }
    }

    private void calculateRatio(String symbol,String clientOrderId, BigDecimal futurePrice, BigDecimal spotPrice, int priceSize) {
        BigDecimal ratio;
        if (clientOrderId.contains(BeanConstant.FUTURE_SELL_OPEN)) {
            ratio = futurePrice.subtract(spotPrice).divide(spotPrice, priceSize,RoundingMode.HALF_UP);
            PairsTradeModel pairsTradeModel =pairsTradeDao.getPairsTradeByOpenId(clientOrderId);
            if(pairsTradeModel == null) {
                pairsTradeModel = new PairsTradeModel();
                pairsTradeModel.setSymbol(symbol);
                pairsTradeModel.setOpenId(clientOrderId);
                pairsTradeModel.setOpenRatio(ratio);
                pairsTradeDao.insertPairsTrade(pairsTradeModel);
            }else{
                pairsTradeDao.updateOpenRatioByOpenId(clientOrderId, ratio);
            }

        } else if (clientOrderId.contains(BeanConstant.FUTURE_SELL_CLOSE)) {
            ratio = spotPrice.subtract(futurePrice).divide(futurePrice, priceSize, RoundingMode.HALF_UP);
            pairsTradeDao.updateCloseRatioByCloseId(clientOrderId, ratio);
        }
    }
}
