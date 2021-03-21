package com.furiousTidy.magicbean.subscription;

/*
* 工具类，缓存行情
* */

import com.binance.client.model.trade.AccountInformation;
import com.binance.client.model.trade.Asset;
import com.binance.client.model.trade.Position;
import com.binance.client.model.user.BalanceUpdate;
import com.binance.client.model.user.OrderUpdate;
import com.binance.client.model.user.PositionUpdate;
import com.furiousTidy.magicbean.dbutil.dao.PairsTradeDao;
import com.furiousTidy.magicbean.dbutil.dao.TradeInfoDao;
import com.furiousTidy.magicbean.dbutil.dao.TradeInfoService;
import com.furiousTidy.magicbean.dbutil.model.TradeInfoModel;
import com.furiousTidy.magicbean.trader.TradeUtil;
import com.furiousTidy.magicbean.util.BeanConstant;
import com.furiousTidy.magicbean.util.BinanceClient;
import com.furiousTidy.magicbean.util.MarketCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.concurrent.locks.Lock;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

@Service
public class FutureSubscription {

    static Logger logger = LoggerFactory.getLogger(FutureSubscription.class);

    @Autowired
    TradeInfoDao tradeInfoDao;

    @Autowired
    PairsTradeDao pairsTradeDao;

    @Autowired
    TradeInfoService tradeInfoService;

    //subscribe funding rate and store in the tree map
    public void fundingRateSub(){
        BinanceClient.futureSubsptClient.subscribeMarkPricesEvent(listMarkPrice -> {
            listMarkPrice.forEach(markPriceEvent -> {
                MarketCache.futureRateCache.put(markPriceEvent.getSymbol(), markPriceEvent.getFundingRate());
                MarketCache.fRateSymbolCache.put(markPriceEvent.getFundingRate(),markPriceEvent.getSymbol());
            });
        },null);
    }

    public void getAllBookTikcers(){
        BinanceClient.futureSyncClient.getSymbolOrderBookTicker(null).forEach(symbolOrderBook -> {
            HashMap map = new HashMap();
            map.put(BeanConstant.BEST_ASK_PRICE,symbolOrderBook.getAskPrice());
            map.put(BeanConstant.BEST_ASK_Qty,symbolOrderBook.getAskQty());
            map.put(BeanConstant.BEST_BID_PRICE,symbolOrderBook.getBidPrice());
            map.put(BeanConstant.BEST_BID_QTY,symbolOrderBook.getBidQty());
            MarketCache.futureTickerMap.put(symbolOrderBook.getSymbol(),map);
        });
    }

    //存储合约最佳挂单行情
    public void allBookTickerSubscription(){
        getAllBookTikcers();
        BinanceClient.futureSubsptClient.subscribeAllBookTickerEvent((symbolBookTickerEvent)->{
            HashMap map = new HashMap();
            map.put(BeanConstant.BEST_ASK_PRICE,symbolBookTickerEvent.getBestAskPrice());
            map.put(BeanConstant.BEST_ASK_Qty,symbolBookTickerEvent.getBestAskQty());
            map.put(BeanConstant.BEST_BID_PRICE,symbolBookTickerEvent.getBestBidPrice());
            map.put(BeanConstant.BEST_BID_QTY,symbolBookTickerEvent.getBestBidQty());
            MarketCache.futureTickerMap.put(symbolBookTickerEvent.getSymbol(),map);

        },null);
    }

    //initial user's future accout info
    public void processFutureCache(){
        //初始化合约缓存
        AccountInformation accountInformation = BinanceClient.futureSyncClient.getAccountInformation();
        for(Asset asset: accountInformation.getAssets()){
            BalanceUpdate balanceUpdate = new BalanceUpdate();
            balanceUpdate.setAsset(asset.getAsset());
            balanceUpdate.setWalletBalance(asset.getAvailableBalance());
            MarketCache.futureBalanceCache.put(asset.getAsset(),balanceUpdate);
        }

        for(Position position:accountInformation.getPositions()){
            PositionUpdate positionUpdate = new PositionUpdate();
            positionUpdate.setSymbol(position.getSymbol());
            positionUpdate.setAmount(new BigDecimal(position.getPositionAmt()));
            positionUpdate.setEntryPrice(new BigDecimal(position.getEntryPrice()));
            MarketCache.futurePositionCache.put(position.getSymbol(),positionUpdate);
        }
        userDataUpdateSubscription();
    }

    //订阅合约的用户状态更新事件，生成合约相关缓存
    public  void userDataUpdateSubscription(){

        // Start user data stream
        String listenKey = BinanceClient.futureSyncClient.startUserDataStream();
        logger.info("获得listenKey: " + listenKey);

        BinanceClient.futureSubsptClient.subscribeUserDataEvent(listenKey, ((event) -> {
            //更新资金、持仓信息
            if(event.getEventType().equals("ACCOUNT_UPDATE")){
                for(BalanceUpdate balanceUpdate:event.getAccountUpdate().getBalances()){
                    MarketCache.futureBalanceCache.put(balanceUpdate.getAsset(),balanceUpdate);
                }
                for(PositionUpdate positionUpdate:event.getAccountUpdate().getPositions()){
                    MarketCache.futurePositionCache.put(positionUpdate.getSymbol(),positionUpdate);
                }
//                logger.info("future accout_update event:{}",event);
                //更新订单信息
            }else if(event.getEventType().equals("ORDER_TRADE_UPDATE")){
                OrderUpdate orderUpdate = event.getOrderUpdate();
                MarketCache.futureOrderCache.put(orderUpdate.getOrderId(),orderUpdate);
                logger.info("future trade_update event: orderstatus={},clientId={},price={},qty={},event={}",
                        orderUpdate.getOrderStatus(),orderUpdate.getClientOrderId(),orderUpdate.getAvgPrice(),orderUpdate.getCumulativeFilledQty(),event);
                if(orderUpdate.getOrderStatus().equals("FILLED") || orderUpdate.getOrderStatus().equals("PARTIALLY_FILLED")){
                    // update the database
                    try {
                        String clientOrderId = orderUpdate.getClientOrderId();
                        //we need a lock to lock pairs trade, or insert may exception
                        Lock eventLock = MarketCache.eventLockCache.get(clientOrderId);
                        if(eventLock.tryLock(2000,MILLISECONDS)){
                            long lockbegin = System.currentTimeMillis();
                            TradeInfoModel tradeInfo =  tradeInfoDao.getTradeInfoByOrderId(orderUpdate.getClientOrderId());
                            if(tradeInfo == null){
                                tradeInfo = new TradeInfoModel();
                                tradeInfo.setSymbol(orderUpdate.getSymbol());
                                tradeInfo.setOrderId(clientOrderId);
                                tradeInfo.setFuturePrice(orderUpdate.getAvgPrice());
                                tradeInfo.setFutureQty(orderUpdate.getCumulativeFilledQty());
                                tradeInfo.setCreateTime(TradeUtil.getCurrentTime());
                                tradeInfoDao.insertTradeInfo(tradeInfo);
                            }
                            else{
                                BigDecimal futurePrice, futureQty;
                                BigDecimal ratio;
                                int priceSize = orderUpdate.getAvgPrice().toString().length() - orderUpdate.getAvgPrice().toString().indexOf(".");
                                //calculate bid price
                                if(tradeInfo.getFuturePrice() == null){
                                    futurePrice = orderUpdate.getAvgPrice();
                                    futureQty = orderUpdate.getCumulativeFilledQty();
                                }else{
                                    futureQty = orderUpdate.getCumulativeFilledQty().add(tradeInfo.getFutureQty());
                                    futurePrice = orderUpdate.getAvgPrice().multiply(orderUpdate.getCumulativeFilledQty())
                                            .add(tradeInfo.getFuturePrice().multiply(tradeInfo.getFutureQty()))
                                            .divide(futureQty,priceSize);
                                }
                                //calcualte ratio
                                if (tradeInfo.getSpotPrice() != null) {
                                    BigDecimal spotPrice = tradeInfo.getSpotPrice();
                                    if(clientOrderId.contains(BeanConstant.FUTURE_SELL_OPEN)) {
                                        ratio = futurePrice.subtract(spotPrice).divide(spotPrice, priceSize,RoundingMode.HALF_UP);
                                        pairsTradeDao.updateOpenRatioByOpenId(clientOrderId,ratio);
                                    }else if(clientOrderId.contains(BeanConstant.FUTURE_SELL_CLOSE)){
                                        ratio = spotPrice.subtract(futurePrice).divide(futurePrice, priceSize,RoundingMode.HALF_UP);
                                        pairsTradeDao.updateCloseRatioByCloseId(clientOrderId, ratio);
                                    }
                                }

                                tradeInfo.setFutureQty(futureQty);
                                tradeInfo.setFuturePrice(futurePrice);

                                tradeInfoDao.updateTradeInfoById(tradeInfo);
                                logger.info("future even has been finished:cost={}",System.currentTimeMillis()-lockbegin);

                            }

                        }
                        eventLock.unlock();
                    } catch (Exception e) {
                        logger.error("future process......failed {}",e);
                    }
                }
            }else if(event.getEventType().equals("LISTEN_KEY_EXPIRED")){
                //Listen key 失效了
                logger.error("listen key expired");
                userDataUpdateSubscription();
            }
            BinanceClient.futureSyncClient.keepUserDataStream(listenKey);

        }), null);
    }


    public static void main(String[] args) throws InterruptedException {
        FutureSubscription futureSubscription = new FutureSubscription();
        futureSubscription.getAllBookTikcers();

//        FutureSubscription.userDataUpdateSubscription();
//        Thread.sleep(5000);
//        String symbol = "LTCUSDT";
//
//        System.out.println(BinanceClient.futureSyncClient.postOrder(symbol, OrderSide.SELL, null, OrderType.LIMIT, TimeInForce.GTC,
//                "1", MarketCache.futureTickerMap.get(symbol).get(BeanConstant.BEST_BID_PRICE).toString(), null, null, null, null, NewOrderRespType.RESULT));
//        System.out.println(BinanceClient.futureSyncClient.postOrder(symbol, OrderSide.BUY, null, OrderType.LIMIT, TimeInForce.GTC,
//                "1", MarketCache.futureTickerMap.get(symbol).get(BeanConstant.BEST_ASK_PRICE).toString(), null, null, null, null, NewOrderRespType.RESULT));
    }



}
