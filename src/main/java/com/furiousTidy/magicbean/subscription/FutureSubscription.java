package com.furiousTidy.magicbean.subscription;

/*
* 工具类，缓存行情
* */

import com.binance.client.SubscriptionErrorHandler;
import com.binance.client.exception.BinanceApiException;
import com.binance.client.model.market.MarkPrice;
import com.binance.client.model.trade.AccountInformation;
import com.binance.client.model.trade.Asset;
import com.binance.client.model.trade.Position;
import com.binance.client.model.user.BalanceUpdate;
import com.binance.client.model.user.OrderUpdate;
import com.binance.client.model.user.PositionUpdate;
import com.furiousTidy.magicbean.dbutil.dao.PairsTradeDao;
import com.furiousTidy.magicbean.dbutil.dao.TradeInfoDao;
import com.furiousTidy.magicbean.dbutil.dao.TradeInfoService;
import com.furiousTidy.magicbean.dbutil.model.TradeInfoModel;
import com.furiousTidy.magicbean.trader.TradeUtil;
import com.furiousTidy.magicbean.trader.service.PositionOpenService;
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
import java.util.List;
import java.util.concurrent.locks.Lock;

import static com.furiousTidy.magicbean.util.MarketCache.futureRateCache;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

@Service
public class FutureSubscription {

    static Logger logger = LoggerFactory.getLogger(FutureSubscription.class);

    @Autowired
    TradeInfoDao tradeInfoDao;

    @Autowired
    PairsTradeDao pairsTradeDao;

    @Autowired
    TradeInfoService tradeInfoService;

    @Autowired
    PositionOpenService positionOpenService;

    //subscribe funding rate and store in the tree map
    public void fundingRateSub(){
        List<MarkPrice> markPriceList = BinanceClient.futureSyncClient.getMarkPrice(null);
        markPriceList.stream().filter(markPrice -> markPrice.getSymbol().contains("USDT"))
                .forEach(markPrice -> {
                    futureRateCache.put(markPrice.getSymbol(), markPrice.getLastFundingRate());
                    MarketCache.fRateSymbolCache.put(markPrice.getLastFundingRate(),markPrice.getSymbol());
                });

        BinanceClient.futureSubsptClient.subscribeMarkPricesEvent(listMarkPrice -> {
            MarketCache.fRateSymbolCache.clear();
            listMarkPrice.forEach(markPriceEvent -> {
                futureRateCache.put(markPriceEvent.getSymbol(), markPriceEvent.getFundingRate());
                MarketCache.fRateSymbolCache.put(markPriceEvent.getFundingRate(),markPriceEvent.getSymbol());
            });
        },null);
    }

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
        BinanceClient.futureSubsptClient.subscribeAllBookTickerEvent((symbolBookTickerEvent) -> {
            if (!symbolBookTickerEvent.getSymbol().contains("USDT")) return;

            HashMap map = new HashMap();
            map.put(BeanConstant.BEST_ASK_PRICE, symbolBookTickerEvent.getBestAskPrice());
            map.put(BeanConstant.BEST_ASK_Qty, symbolBookTickerEvent.getBestAskQty());
            map.put(BeanConstant.BEST_BID_PRICE, symbolBookTickerEvent.getBestBidPrice());
            map.put(BeanConstant.BEST_BID_QTY, symbolBookTickerEvent.getBestBidQty());
            MarketCache.futureTickerMap.put(symbolBookTickerEvent.getSymbol(), map);
//            if (BeanConstant.watchdog
//                    &&symbolBookTickerEvent.getBestBidPrice()!=null && symbolBookTickerEvent.getBestAskPrice()!= null
//                    && MarketCache.spotTickerMap.containsKey(symbolBookTickerEvent.getSymbol())) {
//                try {
//
//                    positionOpenService.processPairsTrade(symbolBookTickerEvent.getSymbol(),symbolBookTickerEvent.getBestBidPrice(), symbolBookTickerEvent.getBestAskPrice()
//                            , MarketCache.spotTickerMap.get(symbolBookTickerEvent.getSymbol()).get(BeanConstant.BEST_BID_PRICE)
//                            , MarketCache.spotTickerMap.get(symbolBookTickerEvent.getSymbol()).get(BeanConstant.BEST_ASK_PRICE));
//                } catch (Exception e) {
//                    logger.error("do future pairs trade exception={}", e);
//                }
//            }


        }, new SubscriptionErrorHandler() {
            @Override
            public void onError(BinanceApiException exception) {
                logger.error("future sub all tick exctpion={}",exception);
            }
        });
    }

    //initial user's future accout info
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
            //更新资金、持仓信息
            if(event.getEventType().equals("ACCOUNT_UPDATE")){
                logger.info("future sub account_update event={}",event);
                for(BalanceUpdate balanceUpdate:event.getAccountUpdate().getBalances()){
                    MarketCache.futureBalanceCache.put(balanceUpdate.getAsset(),balanceUpdate);
                }
                for(PositionUpdate positionUpdate:event.getAccountUpdate().getPositions()){
                    MarketCache.futurePositionCache.put(positionUpdate.getSymbol(),positionUpdate);
                }
//                logger.info("future accout_update event:{}",event);
                //更新订单信息
            }else if(event.getEventType().equals("ORDER_TRADE_UPDATE")){
                OrderUpdate orderUpdate = event.getOrderUpdate();
//                MarketCache.futureOrderCache.put(orderUpdate.getOrderId(),orderUpdate);
//                logger.info("future trade_update event: orderstatus={},clientId={},price={},qty={},event={}",
//                        orderUpdate.getOrderStatus(),orderUpdate.getClientOrderId(),orderUpdate.getAvgPrice(),orderUpdate.getCumulativeFilledQty(),event);
                if(orderUpdate.getOrderStatus().equals("FILLED") || orderUpdate.getOrderStatus().equals("PARTIALLY_FILLED")){
                    // update the database

                }
            }else if(event.getEventType().equals("LISTEN_KEY_EXPIRED")){
                //Listen key 失效了
                logger.error("future sub listen key expired");
                userDataUpdateSubscription();
            }else{
                logger.info("future other event={}",event);
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
