package com.furiousTidy.magicbean.subscription;


import com.binance.api.client.domain.OrderStatus;
import com.binance.api.client.domain.account.Account;
import com.binance.api.client.domain.account.AssetBalance;
import com.binance.api.client.domain.event.OrderTradeUpdateEvent;
import com.furiousTidy.magicbean.apiproxy.SpotSyncClientProxy;
import com.furiousTidy.magicbean.config.BeanConfig;
import com.furiousTidy.magicbean.dbutil.dao.PairsTradeDao;
import com.furiousTidy.magicbean.dbutil.dao.TradeInfoDao;
import com.furiousTidy.magicbean.trader.service.PositionOpenService;
import com.furiousTidy.magicbean.util.BeanConstant;
import com.furiousTidy.magicbean.util.BinanceClient;
import com.furiousTidy.magicbean.util.BookTickerModel;
import com.furiousTidy.magicbean.util.MarketCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;

import static com.binance.api.client.domain.event.UserDataUpdateEvent.UserDataUpdateEventType.ACCOUNT_POSITION_UPDATE;
import static com.binance.api.client.domain.event.UserDataUpdateEvent.UserDataUpdateEventType.ORDER_TRADE_UPDATE;

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

    @Autowired
    PositionOpenService positionOpenService;

    @Autowired
    BinanceClient binanceClient;

//    public void symbolBookTickSubscription(String symbol){
//
//        binanceClient.getSpotSubsptClient().onBookTickerEvent(symbol, bookTickerEvent -> {
//            HashMap map = new HashMap();
//            map.put(BeanConstant.BEST_ASK_PRICE,bookTickerEvent.getAskPrice());
//            map.put(BeanConstant.BEST_ASK_Qty,bookTickerEvent.getAskQuantity());
//            map.put(BeanConstant.BEST_BID_PRICE,bookTickerEvent.getBidPrice());
//            map.put(BeanConstant.BEST_BID_QTY,bookTickerEvent.getBidQuantity());
//            MarketCache.spotTickerMap.put(bookTickerEvent.getSymbol(),map);
//
////            System.out.println("spot event"+bookTickerEvent.toString());
//        });
//    }

    //init booktick cache
    public void getAllBookTicks(){


        spotSyncClientProxy.getAllBookTickers().forEach(bookTicker -> {
           if(bookTicker.getSymbol().contains("USDT")){
               BookTickerModel bookTickerModel = new BookTickerModel();
               bookTickerModel.setSymbol(bookTicker.getSymbol());
               bookTickerModel.setAskPrice(new BigDecimal(bookTicker.getAskPrice()));
               bookTickerModel.setBidPrice(new BigDecimal(bookTicker.getBidPrice()));
               MarketCache.spotTickerMap.put(bookTicker.getSymbol(), bookTickerModel);
           }

        });
    }


//    public void subAllTickByTrade(){
//        getAllBookTicks();
//        final String[] symbols ={""};
//        MarketCache.spotTickerMap.forEach((symbol,map)->{
//            symbols[0] += symbol + ",";
//        });
//        binanceClient.getSpotSubsptClient().onTradeEvent(symbols[0], tradeEvent->{
////            tradeEvents.forEach(tradeEvent -> {
//            BookTickerModel bookTickerModel = new BookTickerModel();
//            bookTickerModel.setTradeTime(tradeEvent.getTradeTime());
//            bookTickerModel.setSymbol(tradeEvent.getSymbol());
//            bookTickerModel.setAskPrice(tradeEvent.getPrice());
//            bookTickerModel.setBidPrice(tradeEvent.getPrice());
//            MarketCache.spotTickerMap.put(bookTickerModel.getSymbol(), bookTickerModel);
////            });
//        });
//    }


    //使用
//    @Async
//    public void subAllTickByDepth(){
//        getAllBookTicks();
//        final String[] symbols ={""};
//        MarketCache.spotTickerMap.forEach((symbol,map)->{
//            symbols[0] += symbol + ",";
//        });
//        logger.info("symbols={}",symbols);
//        binanceClient.getSpotSubsptClient().onDepthEvent(symbols[0],depthEvent->{
//            BookTickerModel bookTickerModel = new BookTickerModel();
//            bookTickerModel.setSymbol(depthEvent.getSymbol());
//            bookTickerModel.setTradeTime(depthEvent.getEventTime());
//            bookTickerModel.setAskPrice(new BigDecimal(depthEvent.getAsks().get(2).getPrice()));
//            bookTickerModel.setBidPrice(new BigDecimal(depthEvent.getBids().get(2).getPrice()));
//            MarketCache.spotTickerMap.put(bookTickerModel.getSymbol(), bookTickerModel);
//        });
//    }


        @Async
    public void allTickSub() throws InterruptedException {
        while(true){
            getAllBookTicks();
            Thread.sleep(BeanConfig.SPOT_SLEEP_TIME);
        }
    }

//    public void allTickSub(){
//        MarketCache.spotTickerMap.forEach((symbol,map)->{
//            binanceClient.getSpotSubsptClient().onTickerEvent(symbol, tickerEvent->{
//                if(symbol.contains("USDT") || ){
//                    MarketCache.spotTickerMap.put(symbol,tickerEvent);
//                }
//            });
//        });
//
//    }

    //订阅现货最新价格
    public void allBookTickSubscription(){

        getAllBookTicks();

        //subscribe bookticker
        binanceClient.getSpotSubsptClient().onAllBookTickersEvent(bookTickerEvent -> {
            if (!bookTickerEvent.getSymbol().contains("USDT")) return;


            BookTickerModel bookTickerModel = new BookTickerModel();
            bookTickerModel.setSymbol(bookTickerEvent.getSymbol());
            bookTickerModel.setTradeTime(System.currentTimeMillis());
            bookTickerModel.setAskPrice(new BigDecimal(bookTickerEvent.getAskPrice().replaceAll("0+$", "")));
            bookTickerModel.setBidPrice(new BigDecimal(bookTickerEvent.getBidPrice().replaceAll("0+$", "")));
            bookTickerModel.setAskQuantity(new BigDecimal(bookTickerEvent.getAskQuantity().replaceAll("0+$", "")));
            bookTickerModel.setBidQuantity(new BigDecimal(bookTickerEvent.getBidQuantity().replaceAll("0+$", "")));
            MarketCache.spotTickerMap.put(bookTickerEvent.getSymbol(),bookTickerModel);

//            if(BeanConstant.watchdog
//                    && bookTickerEvent.getBidPrice() != null && bookTickerEvent.getBidPrice() != null
//                    && MarketCache.futureTickerMap.containsKey(bookTickerEvent.getSymbol())){
//                try {
//                    positionOpenService.processPairsTrade(bookTickerEvent.getSymbol(),
//                            MarketCache.futureTickerMap.get(bookTickerEvent.getSymbol()).get(BeanConstant.BEST_BID_PRICE)
//                            ,MarketCache.futureTickerMap.get(bookTickerEvent.getSymbol()).get(BeanConstant.BEST_ASK_PRICE)
//                            ,new BigDecimal(bookTickerEvent.getBidPrice())
//                            ,new BigDecimal(bookTickerEvent.getAskPrice())
//                    );
//                } catch (InterruptedException e) {
//                    logger.error("do spot pairs trade exception={}",e);
//                }
//            }

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
        Account account = binanceClient.getSpotSyncClient().getAccount();
        for (AssetBalance assetBalance : account.getBalances()) {
            MarketCache.spotBalanceCache.put(assetBalance.getAsset(), assetBalance);
        }
        return binanceClient.getSpotSyncClient().startUserDataStream();
    }

    /**
     * Begins streaming of agg trades events.
     */
    private void startAccountBalanceEventStreaming(String listenKey) {
        binanceClient.getSpotSubsptClient().onUserDataUpdateEvent(listenKey, response -> {
            if (response.getEventType() == ACCOUNT_POSITION_UPDATE) {
                // Override cached asset balances
                for (AssetBalance assetBalance : response.getAccountUpdateEvent().getBalances()) {
                    MarketCache.spotBalanceCache.put(assetBalance.getAsset(), assetBalance);
                }
//                logger.info("spot account_update event:type={},response={}",response.getAccountUpdateEvent().getEventType(),response.toString());

            } else if(response.getEventType() == ORDER_TRADE_UPDATE) {
                OrderTradeUpdateEvent orderUpdate = response.getOrderTradeUpdateEvent();
//                MarketCache.spotOrderCache.put(orderUpdate.getOrderId(), orderUpdate);

                if(orderUpdate.getOrderStatus()==OrderStatus.FILLED ||
                        orderUpdate.getOrderStatus() == OrderStatus.PARTIALLY_FILLED){
//                    logger.info("spot order_update event:type={},orderid={},status={},price={},qty={},response={}",response.getOrderTradeUpdateEvent().getEventType(),
//                            orderUpdate.getNewClientOrderId(),orderUpdate.getOrderStatus(),orderUpdate.getPrice(),orderUpdate.getAccumulatedQuantity(),response.toString());

                }
            }
            logger.info("Waiting for spot balance or order events......");
            binanceClient.getSpotSyncClient().keepAliveUserDataStream(listenKey);
        });
     }

     public static void main(String[] args) throws InterruptedException {

         String price = "5.0310000";
         String newStr = price.replaceAll("0+$", "");
         System.out.println(newStr);



     }
}




