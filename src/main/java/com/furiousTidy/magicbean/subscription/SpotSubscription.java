package com.furiousTidy.magicbean.subscription;


import com.binance.api.client.domain.OrderStatus;
import com.binance.api.client.domain.account.Account;
import com.binance.api.client.domain.account.AssetBalance;
import com.binance.api.client.domain.event.OrderTradeUpdateEvent;
import com.furiousTidy.magicbean.apiproxy.SpotSyncClientProxy;
import com.furiousTidy.magicbean.dbutil.dao.PairsTradeDao;
import com.furiousTidy.magicbean.dbutil.dao.TradeInfoDao;
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

import static com.binance.api.client.domain.event.UserDataUpdateEvent.UserDataUpdateEventType.ACCOUNT_POSITION_UPDATE;
import static com.binance.api.client.domain.event.UserDataUpdateEvent.UserDataUpdateEventType.ORDER_TRADE_UPDATE;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

//现货订阅类
@Service
public class SpotSubscription {

    static Logger logger = LoggerFactory.getLogger(SpotSubscription.class);

    @Autowired
    SpotSyncClientProxy spotSyncClientProxy;


    @Autowired
    TradeInfoDao tradeInfoDao;

    @Autowired
    PairsTradeDao pairsTradeDao;

    public void symbolBookTickSubscription(String symbol){

        BinanceClient.spotSubsptClient.onBookTickerEvent(symbol,bookTickerEvent -> {
            HashMap map = new HashMap();
            map.put(BeanConstant.BEST_ASK_PRICE,bookTickerEvent.getAskPrice());
            map.put(BeanConstant.BEST_ASK_Qty,bookTickerEvent.getAskQuantity());
            map.put(BeanConstant.BEST_BID_PRICE,bookTickerEvent.getBidPrice());
            map.put(BeanConstant.BEST_BID_QTY,bookTickerEvent.getBidQuantity());
            MarketCache.spotTickerMap.put(bookTickerEvent.getSymbol(),map);

//            System.out.println("spot event"+bookTickerEvent.toString());
        });
    }

    //init booktick cache
    public void getAllBookTicks(){
        spotSyncClientProxy.getAllBookTickers().forEach(bookTicker -> {
            HashMap map = new HashMap();
            map.put(BeanConstant.BEST_ASK_PRICE,new BigDecimal(bookTicker.getAskPrice()));
            map.put(BeanConstant.BEST_ASK_Qty,new BigDecimal(bookTicker.getAskQty()));
            map.put(BeanConstant.BEST_BID_PRICE,new BigDecimal(bookTicker.getBidPrice()));
            map.put(BeanConstant.BEST_BID_QTY,new BigDecimal(bookTicker.getBidQty()));
            MarketCache.spotTickerMap.put(bookTicker.getSymbol(),map);
        });
    }

    //订阅现货最新价格
    public void allBookTickSubscription(){

        getAllBookTicks();

        //subscribe bookticker
        BinanceClient.spotSubsptClient.onAllBookTickersEvent(bookTickerEvent -> {
            HashMap map = new HashMap();
            map.put(BeanConstant.BEST_ASK_PRICE,new BigDecimal(bookTickerEvent.getAskPrice()));
            map.put(BeanConstant.BEST_ASK_Qty,new BigDecimal(bookTickerEvent.getAskQuantity()));
            map.put(BeanConstant.BEST_BID_PRICE,new BigDecimal(bookTickerEvent.getBidPrice()));
            map.put(BeanConstant.BEST_BID_QTY,new BigDecimal(bookTickerEvent.getBidQuantity()));
            MarketCache.spotTickerMap.put(bookTickerEvent.getSymbol(),map);
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
//                logger.info("spot account_update event:type={},response={}",response.getAccountUpdateEvent().getEventType(),response.toString());

            } else if(response.getEventType() == ORDER_TRADE_UPDATE) {
                OrderTradeUpdateEvent orderUpdate = response.getOrderTradeUpdateEvent();
                MarketCache.spotOrderCache.put(orderUpdate.getOrderId(), orderUpdate);

                if(orderUpdate.getOrderStatus()==OrderStatus.FILLED ||
                        orderUpdate.getOrderStatus() == OrderStatus.PARTIALLY_FILLED){
//                    logger.info("spot order_update event:type={},orderid={},status={},price={},qty={},response={}",response.getOrderTradeUpdateEvent().getEventType(),
//                            orderUpdate.getNewClientOrderId(),orderUpdate.getOrderStatus(),orderUpdate.getPrice(),orderUpdate.getAccumulatedQuantity(),response.toString());

                }
            }
            logger.info("Waiting for spot balance or order events......");
            BinanceClient.spotSyncClient.keepAliveUserDataStream(listenKey);
        });
     }

     public static void main(String[] args) throws InterruptedException {

         String price = "5.031";
         String qty="2.33";
         BigDecimal oldPrice =  new BigDecimal("5.03");
         BigDecimal oldQty = new BigDecimal("0.65");
         BigDecimal allqty = new BigDecimal("2.98");
         System.out.println(new BigDecimal(price).multiply(new BigDecimal(qty))
                 .add(oldPrice.multiply(oldQty))
                 .divide(allqty,3,RoundingMode.HALF_UP));
     }



}




