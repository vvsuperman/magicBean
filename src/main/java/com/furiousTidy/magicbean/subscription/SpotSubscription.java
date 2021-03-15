package com.furiousTidy.magicbean.subscription;


import com.binance.api.client.domain.OrderStatus;
import com.binance.api.client.domain.account.Account;
import com.binance.api.client.domain.account.AssetBalance;
import com.binance.api.client.domain.event.OrderTradeUpdateEvent;
import com.furiousTidy.magicbean.apiproxy.SpotSyncClientProxy;
import com.furiousTidy.magicbean.dbutil.PairsTradeDao;
import com.furiousTidy.magicbean.dbutil.PairsTradeModel;
import com.furiousTidy.magicbean.dbutil.TradeInfoDao;
import com.furiousTidy.magicbean.dbutil.TradeInfoModel;
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
                logger.info("spot account_update event:type={},response={}",response.getAccountUpdateEvent().getEventType(),response.toString());

            } else if(response.getEventType() == ORDER_TRADE_UPDATE) {
                OrderTradeUpdateEvent orderUpdate = response.getOrderTradeUpdateEvent();
                MarketCache.spotOrderCache.put(orderUpdate.getOrderId(), orderUpdate);

                if(orderUpdate.getOrderStatus()==OrderStatus.FILLED &&
                        orderUpdate.getOrderStatus() == OrderStatus.PARTIALLY_FILLED){
                    logger.info("spot order_update event:type={},orderid={},response={}"+response.getOrderTradeUpdateEvent().getEventType(),
                            orderUpdate.getOrderId(),response.toString());

                    String clientOrderId = orderUpdate.getNewClientOrderId();
                    TradeInfoModel tradeInfo =  tradeInfoDao.getTradeInfoById(clientOrderId);

                    if(tradeInfo == null){
                        tradeInfo = new TradeInfoModel();
                        tradeInfo.setSymbol(orderUpdate.getSymbol());
                        tradeInfo.setOrderId(clientOrderId);
                        tradeInfo.setSpotPrice(new BigDecimal(orderUpdate.getPrice()));
                        tradeInfo.setSpotQty(new BigDecimal(orderUpdate.getAccumulatedQuantity()));
                        tradeInfo.setCreateTime(TradeUtil.getCurrentTime());
                        pairsTradeDao.insertPairsTrade(tradeInfo);
                    }
                    else{
                        BigDecimal spotPrice, spotQty;
                        BigDecimal ratio;
                        String price = orderUpdate.getPrice();
                        String qty = orderUpdate.getAccumulatedQuantity();
                        int priceSize = price.length() - price.indexOf(".");
                        //calculate bid price
                        if(tradeInfo.getSpotPrice() == null){
                            spotPrice = new BigDecimal(price);
                            spotQty = new BigDecimal(qty);
                        }else{
                            spotQty = new BigDecimal(qty).add(tradeInfo.getSpotQty());
                            spotPrice = new BigDecimal(price).multiply(new BigDecimal(qty))
                                    .add(tradeInfo.getSpotPrice()).multiply(tradeInfo.getSpotQty())
                                            .divide(spotQty,priceSize,RoundingMode.HALF_UP);
                        }
                        //calcualte ratio
                        if (tradeInfo.getFuturePrice() != null) {
                            BigDecimal futurePrice = tradeInfo.getFuturePrice();
                            if(clientOrderId.contains(BeanConstant.FUTURE_SELL_OPEN)) {
                                ratio = futurePrice.subtract(spotPrice).divide(spotPrice, priceSize,RoundingMode.HALF_UP);
                                pairsTradeDao.updateOpenRatioByOpenId(clientOrderId, ratio);
                            }else if (clientOrderId.contains(BeanConstant.FUTURE_SELL_CLOSE)){
                                ratio = spotPrice.subtract(futurePrice).divide(futurePrice, priceSize,RoundingMode.HALF_UP);
                                pairsTradeDao.updateCloseRatioByCloseId(clientOrderId, ratio);
                            }
                        }

                        tradeInfo.setSpotQty(spotQty);
                        tradeInfo.setSpotPrice(spotPrice);

                        tradeInfoDao.updateTradeInfoById(tradeInfo);

                    }

                }

            }
            logger.info("Waiting for spot balance or order events......");
            BinanceClient.spotSyncClient.keepAliveUserDataStream(listenKey);
        });
     }

     public static void main(String[] args) throws InterruptedException {
        String symbol = "btcusdt";
         SpotSubscription spotSubscription = new SpotSubscription();
         spotSubscription.getAllBookTicks();
//         spotSubscription.symbolBookTickSubscription(symbol);
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




