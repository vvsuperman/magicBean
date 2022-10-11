package com.furiousTidy.magicbean.trader.service;

import com.furiousTidy.magicbean.apiproxy.ProxyUtil;
import com.furiousTidy.magicbean.config.BeanConfig;
import com.furiousTidy.magicbean.dbutil.dao.PairsTradeDao;
import com.furiousTidy.magicbean.dbutil.dao.TradeInfoDao;
import com.furiousTidy.magicbean.dbutil.model.PairsTradeModel;
import com.furiousTidy.magicbean.dbutil.model.TradeInfoModel;
import com.furiousTidy.magicbean.util.TradeUtil;
import com.furiousTidy.magicbean.util.BeanConstant;
import com.furiousTidy.magicbean.util.MarketCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
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

    @Autowired
    ProxyUtil proxyUtil;

    @Async
    public void processFutureOrder(String symbol, String clientOrderId, BigDecimal price, BigDecimal qty
            , BigDecimal ratio, long futureTickDelayTime){
        log.info(" future store begin,symbol={}, price={},qty={}",symbol,price,qty);

        Lock eventLock = null;
        try {
            //we need a lock to lock pairs trade, or insert may exception
             eventLock = MarketCache.eventLockCache.containsKey(clientOrderId)?MarketCache.eventLockCache.get(clientOrderId):null;
            if(eventLock == null){
                eventLock = new ReentrantLock();
                MarketCache.eventLockCache.put(clientOrderId, eventLock);
            }
            eventLock.lock();

            TradeInfoModel tradeInfo =  tradeInfoDao.getTradeInfoByOrderId(clientOrderId);
            if(tradeInfo == null){
                tradeInfo = new TradeInfoModel();
                tradeInfo.setSymbol(symbol);
                tradeInfo.setOrderId(clientOrderId);
                tradeInfo.setFuturePrice(price);
                tradeInfo.setFutureQty(qty);
                tradeInfo.setCreateTime(BeanConstant.dateFormat.format(new Date()));
                tradeInfo.setFutureTickDelayTime(futureTickDelayTime);
                tradeInfoDao.insertTradeInfo(tradeInfo);
            }
            else{
                BigDecimal futurePrice, futureQty;
                int priceSize = price.toString().length() - price.toString().indexOf(".");
                //calculate bid price
                if(tradeInfo.getFuturePrice() == null){
                    futurePrice = price;
                    futureQty = qty;
                }else{
                    futureQty = qty.add(tradeInfo.getFutureQty());
                    futurePrice = price.multiply(qty)
                            .add(tradeInfo.getFuturePrice().multiply(tradeInfo.getFutureQty()))
                            .divide(futureQty,priceSize,RoundingMode.HALF_UP);
                }

                tradeInfo.setFutureQty(futureQty);
                tradeInfo.setFuturePrice(futurePrice);
                tradeInfo.setUpdateTime(BeanConstant.dateFormat.format(new Date()));
                tradeInfo.setFutureTickDelayTime(futureTickDelayTime);
                tradeInfoDao.updateTradeInfoById(tradeInfo);

                log.info("future calculate ratio begin, tradeInfo={}", tradeInfo);

                //calcualte ratio
                if (tradeInfo.getSpotPrice() != null) {
                    BigDecimal spotPrice = tradeInfo.getSpotPrice();
                    calculateRatioAndProfit(symbol,clientOrderId,futurePrice,spotPrice,priceSize,ratio);
                }
            }

        } catch (Exception e) {
            log.error("future process......failed {}",e);
        }finally {
            eventLock.unlock();
        }

        //doing some money problem
        if(clientOrderId.contains(BeanConstant.FUTURE_SELL_CLOSE)){
           // proxyUtil.addBalance(price.multiply(qty),"future");
            BeanConstant.closeProcessingSet.remove(clientOrderId);
        }else if(clientOrderId.contains(BeanConstant.FUTURE_SELL_OPEN)){
            BeanConstant.HAS_NEW_TRADE_OPEN.set(true);
        }
    }

    @Async
    public void processSpotOrder(String symbol, String clientOrderId, BigDecimal price, BigDecimal qty
            , BigDecimal orignRatio, long spotTickDelayTime){
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
                tradeInfo.setCreateTime(BeanConstant.dateFormat.format(new Date()));
                tradeInfo.setSpotTickDelayTime(spotTickDelayTime);
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
                tradeInfo.setUpdateTime(BeanConstant.dateFormat.format(new Date()));
                tradeInfo.setSpotTickDelayTime(spotTickDelayTime);
                log.info("spot store process update, tradeInfo {}",tradeInfo);
                tradeInfoDao.updateTradeInfoById(tradeInfo);

                //calcualte ratio
                log.info("spot calculate ratio begin, tradeInfo={}", tradeInfo);
                if (tradeInfo.getFuturePrice() != null) {
                    BigDecimal futurePrice = tradeInfo.getFuturePrice();
                    calculateRatioAndProfit(symbol,clientOrderId, futurePrice, spotPrice, priceSize,orignRatio);
                }
            }
        } catch (Exception e) {
            log.error("spot process......failed {}",e);
        }finally {
            eventLock.unlock();
        }

        if(clientOrderId.contains(BeanConstant.FUTURE_SELL_CLOSE)){
            BeanConstant.ENOUGH_MONEY.set(true);
        }else if(clientOrderId.contains(BeanConstant.FUTURE_SELL_OPEN)){
            BeanConstant.HAS_NEW_TRADE_OPEN.set(true);
        }

    }

    private void  calculateRatioAndProfit(String symbol, String clientOrderId, BigDecimal futurePrice, BigDecimal spotPrice, int priceSize,BigDecimal orginRatio) {
        BigDecimal ratio = BigDecimal.ZERO;
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
                pairsTradeModel.setCreateTime(BeanConstant.dateFormat.format(new Date()));
                pairsTradeModel.setOrigOpenRatio(orginRatio);
//                log.info("insert pairstrade,pairsTrade={}",pairsTradeModel);
                pairsTradeDao.insertPairsTrade(pairsTradeModel);
            }else{
//                log.info("update pairstrade,pairsTrade={}",pairsTradeModel);
                pairsTradeDao.updateOpenRatioByOpenId(clientOrderId, ratio);
            }


        } else if (clientOrderId.contains(BeanConstant.FUTURE_SELL_CLOSE)) {

            PairsTradeModel pairsTradeModel = pairsTradeDao.getPairsTradeByCloseId(clientOrderId);
            if(pairsTradeModel == null){
                log.info("exception: get pairs trade null, clientOrderId={}",clientOrderId);
                return;
            }
            TradeInfoModel tradeInfoModel = tradeInfoDao.getTradeInfoByOrderId(pairsTradeModel.getOpenId());
            profit = tradeUtil.getProfit(tradeInfoModel.getFuturePrice(),tradeInfoModel.getSpotPrice(),futurePrice,spotPrice,tradeInfoModel.getFutureQty());
            ratio = spotPrice.subtract(futurePrice).divide(futurePrice, 6, RoundingMode.HALF_UP);
            pairsTradeModel.setProfit(profit);
            pairsTradeModel.setCloseRatio(ratio);
            pairsTradeModel.setUpdateTime(BeanConstant.dateFormat.format(new Date()));
            pairsTradeModel.setOrigCloseRatio(orginRatio);
            pairsTradeDao.updatePairsTrade(pairsTradeModel);
        }

        //if origRatio has big gap of the ratio, then sleep for a monent
        if(orginRatio != null){
            doAveraging(clientOrderId,orginRatio,ratio);
        }
//        tradeUtil.checkUSDEnough();
    }

    private void doAveraging(String clientOrderId,BigDecimal orginRatio, BigDecimal ratio) {
        if(orginRatio.subtract(ratio).compareTo(BeanConfig.ratioTolerate)>0 && ratio.compareTo(BeanConfig.OPEN_PRICE_GAP)<0 ){
            BeanConstant.GAP_2_BIG = true;
            log.info("gap too big,stop trade clientOrderId={}, orignRatio = {}, ratio ={}", clientOrderId,orginRatio,ratio);
        }
    }


}
