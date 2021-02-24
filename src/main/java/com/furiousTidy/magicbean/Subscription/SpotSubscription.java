package com.furiousTidy.magicbean.Subscription;


import com.binance.api.client.domain.account.Account;
import com.binance.api.client.domain.account.AssetBalance;
import com.binance.api.client.domain.event.OrderTradeUpdateEvent;
import com.furiousTidy.magicbean.util.BeanConstant;
import com.furiousTidy.magicbean.util.BinanceClient;
import com.furiousTidy.magicbean.util.MarketCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

import static com.binance.api.client.domain.event.UserDataUpdateEvent.UserDataUpdateEventType.ACCOUNT_POSITION_UPDATE;
import static com.binance.api.client.domain.event.UserDataUpdateEvent.UserDataUpdateEventType.ORDER_TRADE_UPDATE;

//现货订阅类
public class SpotSubscription {

    static Logger logger = LoggerFactory.getLogger(SpotSubscription.class);

    //订阅现货最新价格
    public void allBookTickSubscription(){
        BinanceClient.spotSubsptClient.onAllBookTickersEvent(bookTickerEvent -> {
            HashMap map = new HashMap();
            map.put(BeanConstant.BEST_ASK_PRICE,bookTickerEvent.getAskPrice());
            map.put(BeanConstant.BEST_ASK_Qty,bookTickerEvent.getAskQuantity());
            map.put(BeanConstant.BEST_BID_PRICE,bookTickerEvent.getBidPrice());
            map.put(BeanConstant.BEST_BID_QTY,bookTickerEvent.getBidQuantity());
            MarketCache.tickerMap.put(bookTickerEvent.getSymbol(),map);

            String symbol = "AVAXUSDT";
            if(MarketCache.tickerMap.containsKey(symbol))
              System.out.println(MarketCache.tickerMap.get(symbol).get(BeanConstant.BEST_ASK_PRICE));
        });
    }

    /**
     * Listen key used to interact with the user data streaming API.
     */

    public void processBalanceCache() {
        String listenKey = initializeAssetBalanceCacheAndStreamSession();
        startAccountBalanceEventStreaming(listenKey);
    }

    /**
     * Initializes the asset balance cache by using the REST API and starts a new user data streaming session.
     *
     * @return a listenKey that can be used with the user data streaming API.
     */
    private String initializeAssetBalanceCacheAndStreamSession() {
        Account account = BinanceClient.spotSyncClient.getAccount();
        for (AssetBalance assetBalance : account.getBalances()) {
            MarketCache.spotBalanceCache.put(assetBalance.getAsset(), assetBalance);
        }

        return BinanceClient.spotSyncClient.startUserDataStream();
    }

    /**
     * Begins streaming of agg trades events.
     */
    private void startAccountBalanceEventStreaming(String listenKey) {
        BinanceClient.spotSubsptClient.onUserDataUpdateEvent(listenKey, response -> {
            if (response.getEventType() == ACCOUNT_POSITION_UPDATE) {
                // Override cached asset balances
                for (AssetBalance assetBalance : response.getAccountUpdateEvent().getBalances()) {
                    MarketCache.spotBalanceCache.put(assetBalance.getAsset(), assetBalance);
                }
            } else if(response.getEventType() == ORDER_TRADE_UPDATE) {
                OrderTradeUpdateEvent orderTradeUpdateEvent = response.getOrderTradeUpdateEvent();
                MarketCache.spotOrderCache.put(orderTradeUpdateEvent.getOrderId(), orderTradeUpdateEvent);

                // We can keep alive the user data stream
            }
            logger.info("Waiting for spot balance or order events......");
            BinanceClient.spotSyncClient.keepAliveUserDataStream(listenKey);
        });
     }

     public static void main(String[] args) throws InterruptedException {
         SpotSubscription spotSubscription = new SpotSubscription();
         spotSubscription.allBookTickSubscription();
//         spotSubscription.processBalanceCache();
//
//         Thread.sleep(5000);
//
//         // Placing a real LIMIT order
//         String symbol = "BNBUSDT";
//         NewOrderResponse newOrderResponse = BinanceClient.spotSyncClient.newOrder(limitBuy(symbol, TimeInForce.GTC, "0.09", "270").newOrderRespType(NewOrderResponseType.FULL));
//         System.out.println(newOrderResponse);
     }



}




