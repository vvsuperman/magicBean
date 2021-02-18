package com.furiousTidy.magicbean.trader;

/*
* 工具类，缓存行情
* */

import com.binance.client.RequestOptions;
import com.binance.client.SubscriptionClient;
import com.binance.client.SyncRequestClient;
import com.furiousTidy.magicbean.constant.BeanConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;


public class MarketSubscription {

    public static Map<String,HashMap<String,BigDecimal>> tickerMap = new HashMap<String, HashMap<String, BigDecimal>>();

    static Logger logger = LoggerFactory.getLogger(MarketSubscription.class);

    //合约客户端
    private static SyncRequestClient syncRequestClient = SyncRequestClient.create(BeanConfig.API_KEY, BeanConfig.SECRET_KEY,
            new RequestOptions());

    private static SubscriptionClient subscriptionClient = SubscriptionClient.create(BeanConfig.API_KEY, BeanConfig.SECRET_KEY);

    //存储合约最佳挂单行情
    public static void bookTickerSubscription(){
        subscriptionClient.subscribeAllBookTickerEvent((symbolBookTickerEvent)->{
            HashMap map = new HashMap();
            map.put(BeanConfig.BEST_ASK_PRICE,symbolBookTickerEvent.getBestAskPrice());
            map.put(BeanConfig.BEST_ASK_Qty,symbolBookTickerEvent.getBestAskQty());
            map.put(BeanConfig.BEST_BID_PRICE,symbolBookTickerEvent.getBestBidPrice());
            map.put(BeanConfig.BEST_BID_QTY,symbolBookTickerEvent.getBestBidQty());
            tickerMap.put(symbolBookTickerEvent.getSymbol(),map);

        },null);
    }


    public static void main(String[] args){
        MarketSubscription.bookTickerSubscription();
    }



}
