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
import com.furiousTidy.magicbean.util.BeanConstant;
import com.furiousTidy.magicbean.util.BinanceClient;
import com.furiousTidy.magicbean.util.MarketCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;

@Service
public class FutureSubscription {

    static Logger logger = LoggerFactory.getLogger(FutureSubscription.class);

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
            logger.info("event:"+event);
            //更新资金、持仓信息
            if(event.getEventType().equals("ACCOUNT_UPDATE")){
                for(BalanceUpdate balanceUpdate:event.getAccountUpdate().getBalances()){
                    MarketCache.futureBalanceCache.put(balanceUpdate.getAsset(),balanceUpdate.getWalletBalance());
                }
                for(PositionUpdate positionUpdate:event.getAccountUpdate().getPositions()){
                    MarketCache.futurePositionCache.put(positionUpdate.getSymbol(),positionUpdate);
                }
                logger.info("future accout_update event:{}",event);
                //更新订单信息
            }else if(event.getEventType().equals("ORDER_TRADE_UPDATE")){
                OrderUpdate orderUpdate = event.getOrderUpdate();
                MarketCache.futureOrderCache.put(orderUpdate.getOrderId(),orderUpdate);
                logger.info("future trade_update event: orderid={},orderstatus={},event={}",orderUpdate.getOrderId(),
                        orderUpdate.getOrderStatus(),event);
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
