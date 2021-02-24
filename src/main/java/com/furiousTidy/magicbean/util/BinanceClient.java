package com.furiousTidy.magicbean.util;

import com.binance.api.client.BinanceApiClientFactory;
import com.binance.api.client.BinanceApiRestClient;
import com.binance.api.client.BinanceApiWebSocketClient;
import com.binance.client.RequestOptions;
import com.binance.client.SubscriptionClient;
import com.binance.client.SyncRequestClient;
import com.furiousTidy.magicbean.constant.BeanConfig;

public class BinanceClient {

    //合约同步客户端
    public static SyncRequestClient futureSyncClient = SyncRequestClient.create(BeanConfig.FUTURE_API_KEY, BeanConfig.FUTURE_SECRET_KEY,
            new RequestOptions());

    //合约订阅客户端
    public static SubscriptionClient futureSubsptClient = SubscriptionClient.create(BeanConfig.FUTURE_API_KEY, BeanConfig.FUTURE_SECRET_KEY);


    private static BinanceApiClientFactory clientFactory = BinanceApiClientFactory.newInstance(BeanConfig.SPOT_API_KEY, BeanConfig.SPOT_SECRET_KEY);
    public static BinanceApiRestClient spotSyncClient = clientFactory.newRestClient();

    public static BinanceApiWebSocketClient spotSubsptClient = clientFactory.newWebSocketClient();

}
