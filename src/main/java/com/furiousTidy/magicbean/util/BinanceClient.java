package com.furiousTidy.magicbean.util;

import com.binance.api.client.BinanceApiClientFactory;
import com.binance.api.client.BinanceApiMarginRestClient;
import com.binance.api.client.BinanceApiRestClient;
import com.binance.api.client.BinanceApiWebSocketClient;
import com.binance.client.RequestOptions;
import com.binance.client.SubscriptionClient;
import com.binance.client.SyncRequestClient;
import com.furiousTidy.magicbean.config.BeanConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

@Component
public class BinanceClient {

//    //合约同步客户端
//    public static SyncRequestClient futureSyncClient = SyncRequestClient.create(BeanConfig.FUTURE_API_KEY, BeanConfig.FUTURE_SECRET_KEY,
//            new RequestOptions());
//
//    //合约订阅客户端
//    public static SubscriptionClient futureSubsptClient = SubscriptionClient.create(BeanConfig.FUTURE_API_KEY, BeanConfig.FUTURE_SECRET_KEY);
//
//
//    //binance spot client
//    private static BinanceApiClientFactory clientFactory = BinanceApiClientFactory.newInstance(BeanConfig.SPOT_API_KEY, BeanConfig.SPOT_SECRET_KEY);
//    public static BinanceApiRestClient spotSyncClient = clientFactory.newRestClient();
//    public static BinanceApiMarginRestClient marginRestClient = clientFactory.newMarginRestClient();

    @Value("api_key")
    String api_key;

    @Value("api_secret")
    String secret_key;

    //合约rest客户端
    volatile static SyncRequestClient futureSyncClient ;

    //合约订阅客户端
    volatile static SubscriptionClient futureSubsptClient;

    public SyncRequestClient getFutureSyncClient(){
        if(futureSyncClient==null){
            futureSyncClient = SyncRequestClient.create(api_key,secret_key, new RequestOptions());
        }
        return futureSyncClient;
    }

    public SubscriptionClient getFutureSubsptClient(){
        if(futureSubsptClient == null){
            futureSubsptClient = SubscriptionClient.create(BeanConfig.API_KEY, BeanConfig.SECRET_KEY);
        }
        return futureSubsptClient;
    }

   public BinanceApiRestClient getSpotSyncClient(){

   }

    //binance spot client
    private static BinanceApiClientFactory clientFactory = BinanceApiClientFactory.newInstance(BeanConfig.API_KEY, BeanConfig.SECRET_KEY);
     static BinanceApiRestClient spotSyncClient = clientFactory.newRestClient();
    public static BinanceApiMarginRestClient marginRestClient = clientFactory.newMarginRestClient();



    public static BinanceApiWebSocketClient spotSubsptClient = clientFactory.newWebSocketClient();

}
