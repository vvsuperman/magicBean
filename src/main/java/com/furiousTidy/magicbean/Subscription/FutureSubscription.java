package com.furiousTidy.magicbean.Subscription;

/*
* 工具类，缓存行情
* */

import com.binance.client.model.enums.*;
import com.binance.client.model.event.MarkPriceEvent;
import com.binance.client.model.trade.AccountInformation;
import com.binance.client.model.trade.Asset;
import com.binance.client.model.trade.Position;
import com.binance.client.model.user.BalanceUpdate;
import com.binance.client.model.user.OrderUpdate;
import com.binance.client.model.user.PositionUpdate;
import com.furiousTidy.magicbean.constant.BeanConstant;
import com.furiousTidy.magicbean.util.BinanceClient;
import com.furiousTidy.magicbean.util.MarketCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;


public class FutureSubscription {

    static Logger logger = LoggerFactory.getLogger(FutureSubscription.class);

    //存储合约最佳挂单行情
    public static void bookTickerSubscription(){

        BinanceClient.futureSubsptClient.subscribeAllBookTickerEvent((symbolBookTickerEvent)->{
            HashMap map = new HashMap();
            map.put(BeanConstant.BEST_ASK_PRICE,symbolBookTickerEvent.getBestAskPrice());
            map.put(BeanConstant.BEST_ASK_Qty,symbolBookTickerEvent.getBestAskQty());
            map.put(BeanConstant.BEST_BID_PRICE,symbolBookTickerEvent.getBestBidPrice());
            map.put(BeanConstant.BEST_BID_QTY,symbolBookTickerEvent.getBestBidQty());
            MarketCache.tickerMap.put(symbolBookTickerEvent.getSymbol(),map);
            logger.info(symbolBookTickerEvent.toString());
        },null);
    }

    //存储实时资金费率
    public static void marketPricesSubscription(){

        BinanceClient.futureSubsptClient.subscribeMarkPricesEvent(((event) -> {
            //将MarkPriceEvent排成有序
            Collections.sort(event, new Comparator<MarkPriceEvent>() {
                public int compare(MarkPriceEvent o1, MarkPriceEvent o2) {
                    return o1.getFundingRate().compareTo(o2.getFundingRate());
                }
            });
            MarketCache.markPriceList = event;
        }), null);
    }

    //存储账户的实时合约持仓，没有订阅接口，只能主动查
//    public void getBalance(){
//        MarketCache.accountInfo = BinanceClient.futureSyncClient.getAccountInformation();
//    }

    public static void processFutureCache(){
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
    public static void userDataUpdateSubscription(){


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
                //更新订单信息
            }else if(event.getEventType().equals("ORDER_TRADE_UPDATE")){
                OrderUpdate orderUpdate = event.getOrderUpdate();
                MarketCache.futureOrderCache.put(orderUpdate.getOrderId(),orderUpdate);
            }else if(event.getEventType().equals("LISTEN_KEY_EXPIRED")){
                //Listen key 失效了
                logger.error("listen key expired");
                userDataUpdateSubscription();
            }

            BinanceClient.futureSyncClient.keepUserDataStream(listenKey);

        }), null);
    }

    //构造持仓
    public void spotOrderUpdateSubscription(){

    }



    public static void main(String[] args) throws InterruptedException {
        FutureSubscription.bookTickerSubscription();
//        FutureSubscription.userDataUpdateSubscription();
//        Thread.sleep(5000);
//        String symbol = "LTCUSDT";
//
//        System.out.println(BinanceClient.futureSyncClient.postOrder(symbol, OrderSide.SELL, null, OrderType.LIMIT, TimeInForce.GTC,
//                "1", MarketCache.tickerMap.get(symbol).get(BeanConstant.BEST_BID_PRICE).toString(), null, null, null, null, NewOrderRespType.RESULT));
//        System.out.println(BinanceClient.futureSyncClient.postOrder(symbol, OrderSide.BUY, null, OrderType.LIMIT, TimeInForce.GTC,
//                "1", MarketCache.tickerMap.get(symbol).get(BeanConstant.BEST_ASK_PRICE).toString(), null, null, null, null, NewOrderRespType.RESULT));
    }



}
