package com.furiousTidy.magicbean.subscription;


import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.binance.api.client.domain.OrderStatus;
import com.binance.api.client.domain.account.Account;
import com.binance.api.client.domain.account.AssetBalance;
import com.binance.api.client.domain.event.OrderTradeUpdateEvent;
import com.binance.api.client.domain.event.TradeEvent;
import com.furiousTidy.magicbean.apiproxy.SpotSyncClientProxy;
import com.furiousTidy.magicbean.config.BeanConfig;
import com.furiousTidy.magicbean.dbutil.dao.PairsTradeDao;
import com.furiousTidy.magicbean.dbutil.dao.TradeInfoDao;
import com.furiousTidy.magicbean.trader.service.PositionOpenService;
import com.furiousTidy.magicbean.util.BeanConstant;
import com.furiousTidy.magicbean.util.BinanceClient;
import com.furiousTidy.magicbean.util.BookTickerModel;
import com.furiousTidy.magicbean.util.MarketCache;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.sql.Array;
import java.util.*;
import java.math.RoundingMode;
import java.util.stream.Collectors;

import static com.binance.api.client.domain.event.UserDataUpdateEvent.UserDataUpdateEventType.ACCOUNT_POSITION_UPDATE;
import static com.binance.api.client.domain.event.UserDataUpdateEvent.UserDataUpdateEventType.ORDER_TRADE_UPDATE;

//现货订阅类
@Service
@Slf4j
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

    @Autowired
    RestTemplate restTemplate;

    String BN_URL_SPOT=
            "https://api.coinmarketcap.com/data-api/v3/exchange/market-pairs/latest?slug=binance&category=spot&limit=100&start=";

    public void subAllTrade(){
        String symbols = "";
        for(String key: MarketCache.spotTickerMap.keySet()){
            symbols += key + ",";
        }

        binanceClient.getSpotSubsptClient().onAllTradeEvent(symbols,tradeEvent->{
                MarketCache.spotTradeCache.put( tradeEvent.getSymbol(), tradeEvent);
        } );
    }


    public Set getSymbolVolume() throws InterruptedException {

        Set<String> volumeSymbolSet = new HashSet<String>();

        HttpHeaders headers = new HttpHeaders();
        HttpEntity entity = new HttpEntity(headers);

        double volume = 0;
        String lastSymbol = "";

        for (int i = 1; i < 500; i += 100) {
            Thread.sleep(100);
            String url = BN_URL_SPOT + i;
            ResponseEntity<JSONObject> returnMsg = null;
            while (returnMsg == null) {
                try {
                    returnMsg = restTemplate.exchange(url, HttpMethod.GET, entity, JSONObject.class);
                    Thread.sleep(100);
                } catch (Exception ex) {
                    logger.error("get spot from coin market error");
                }
            }

            if (returnMsg != null) {

                JSONObject jsonData = returnMsg.getBody().getJSONObject("data");
                JSONArray jsonMarketPairs = jsonData.getJSONArray("marketPairs");

                for (int j = 0; j < jsonMarketPairs.size(); j++) {
                    JSONObject marketObj = (JSONObject) jsonMarketPairs.get(j);
                    String symbol = String.valueOf(marketObj.get("marketPair"));
                    if(symbol.contains("USDT")){
                        symbol = symbol.replaceAll("/","");
                        symbol = symbol.replaceAll("USDT","");
                        symbol += "USDT";
                        lastSymbol = symbol;
                        volumeSymbolSet.add(symbol);
                        volume = (double) marketObj.get("volumeUsd");
                    }

                }
            }
        }
        logger.info("成交量排名set添加完毕，最后一个symbol:"+ lastSymbol + " 成交量:" + volume);
        return volumeSymbolSet;


    }


    public void subTradesByMap() throws InterruptedException {
        String symbols = "";
        List subTradpList = new ArrayList<String>();

        Set volumeSymbolSet = getSymbolVolume();


        MarketCache.spotInfoCache.forEach((key,value)->{
            if(MarketCache.futureInfoCache.containsKey(key) || volumeSymbolSet.contains(key)){
                subTradpList.add(key);
            }
        });

        for(Object key: subTradpList){
            symbols += key + ",";
        }

        binanceClient.getSpotSubsptClient().onAllTradeEvent(symbols,tradeEvent->{
            MarketCache.spotTradeCache.put( tradeEvent.getSymbol(), tradeEvent);
        } );

    }

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

         String symbols="bnbusdt,btcusdt";
         String channel = Arrays.stream(symbols.split(","))
                 .map(String::trim)
                 .map(s -> s.toLowerCase())
                 .map(s -> String.format("%s@trade", s))
                 .collect(Collectors.joining("/"));
         System.out.println(channel);


     }
}




