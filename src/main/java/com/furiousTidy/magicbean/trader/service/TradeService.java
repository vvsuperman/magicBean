package com.furiousTidy.magicbean.trader.service;


import com.binance.api.client.domain.OrderStatus;
import com.binance.api.client.domain.account.NewOrderResponse;
import com.binance.api.client.domain.account.NewOrderResponseType;
import com.binance.api.client.domain.account.request.CancelOrderRequest;
import com.binance.api.client.domain.account.request.CancelOrderResponse;
import com.binance.client.exception.BinanceApiException;
import com.binance.client.model.enums.*;
import com.binance.client.model.trade.Order;
import com.furiousTidy.magicbean.apiproxy.SpotSyncClientProxy;
import com.furiousTidy.magicbean.config.BeanConfig;
import com.furiousTidy.magicbean.dbutil.dao.PairsTradeDao;
import com.furiousTidy.magicbean.dbutil.dao.TradeInfoDao;
import com.furiousTidy.magicbean.dbutil.model.PairsTradeModel;
import com.furiousTidy.magicbean.dbutil.model.TradeInfoModel;
import com.furiousTidy.magicbean.trader.TradeUtil;
import com.furiousTidy.magicbean.util.BeanConstant;
import com.furiousTidy.magicbean.util.BinanceClient;
import com.furiousTidy.magicbean.util.MarketCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.locks.Lock;

import static com.binance.api.client.domain.account.NewOrder.limitBuy;
import static com.binance.api.client.domain.account.NewOrder.limitSell;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

@Service
@Slf4j
public class TradeService {

    @Autowired
    SpotSyncClientProxy spotSyncClientProxy;

    @Autowired
    TradeInfoDao tradeInfoDao;

    @Autowired
    PairsTradeDao pairsTradeDao;

    @Autowired
    OrderStoreService orderStoreService;

    @Async
    public void doFutureTrade(String symbol, BigDecimal futurePrice, BigDecimal futureQty, int futureStepSize,
                              String direct, String clientOrderId) throws InterruptedException{

        OrderSide orderSide =(direct.equals(BeanConstant.FUTURE_SELL_OPEN))? OrderSide.SELL:OrderSide.BUY;
//      PositionSide positionSide = (direct.equals(BeanConstant.FUTURE_SELL_OPEN))?PositionSide.SHORT:PositionSide.LONG;
        PositionSide positionSide = null;
        int i=1;

        while (futureQty.compareTo(BigDecimal.ZERO)>0 && futurePrice.multiply(futureQty).compareTo(BeanConfig.MIN_OPEN_UNIT)>0) {
            //下单
            log.info("new  future order begin {}, symbol={},orderside={},positionside={},futurePrice={},futureQty={},clientId={}"
                    ,i++,symbol,orderSide,positionSide,futurePrice,futureQty,clientOrderId);
            Order order = BinanceClient.futureSyncClient.postOrder(symbol,orderSide,positionSide, OrderType.LIMIT, TimeInForce.GTC,futureQty.toString(),
                    futurePrice.toString(),null,clientOrderId,null,null, NewOrderRespType.RESULT);
            Long orderId = order.getOrderId();
            log.info("futrue new order return: orderid={},status={},qty={}" , orderId,order.getStatus(),order.getExecutedQty());
//            Thread.sleep(BeanConfig.ORDER_EXPIRE_TIME);
            //suscription receive the info
//            if(MarketCache.futureOrderCache.containsKey(orderId) &&
//                    MarketCache.futureOrderCache.get(orderId).getOrderStatus().equals("FILLED")){
//                log.info("future order has been filled: orderid={}",orderId);
//                return;
//            }

            try {
                order = BinanceClient.futureSyncClient.cancelOrder(symbol, order.getOrderId(), null);
            }catch (BinanceApiException binanceApiException){
                if (binanceApiException.getMessage().contains("Unknown order sent")) {
                    //order has been filled but no subscription received, do nothing
                    log.info("future order has been filled,no need cancel,begin process,symbol={},status={},qty={},orderid={}"
                            ,symbol,order.getStatus(),order.getExecutedQty(),orderId);
                    orderStoreService.processFutureOrder(clientOrderId,order);
                    return;
                }
            }

            if(order.getExecutedQty().equals(futureQty)){
                log.info("future order has been filled until the cancel order: orderid={}",orderId);
                return;
            }else{
//                Thread.sleep(BeanConfig.ORDER_EXPIRE_TIME);
                if(direct.equals(BeanConstant.FUTURE_SELL_OPEN)){
                    futurePrice =  MarketCache.futureTickerMap.get(symbol).get(BeanConstant.BEST_BID_PRICE);
                }else if(direct.equals(BeanConstant.FUTURE_SELL_CLOSE)){
                    futurePrice = MarketCache.futureTickerMap.get(symbol).get(BeanConstant.BEST_ASK_PRICE);
                }

                futureQty = futureQty.subtract(order.getExecutedQty().setScale(futureStepSize, RoundingMode.HALF_UP));
                log.info("future's next order info,bidPrice={}, futureQty={}",futurePrice,futureQty);
            }
        }
    }

    @Async
    public void doSpotTrade(String symbol, BigDecimal spotPrice, BigDecimal spotQty, int spotStepSize,String direct,String clientOrderId) throws InterruptedException{
        int i=1;

        while(spotQty.compareTo(BigDecimal.ZERO)>0 &&
                spotPrice.multiply(spotQty).compareTo(BeanConfig.MIN_OPEN_UNIT)>0) {
            NewOrderResponse newOrderResponse = null;
            log.info("new spot order begin {},symbol={},price={},qty={},direct={},clientid={}",i++,symbol,spotPrice,spotQty,direct,clientOrderId);
            if(direct.equals(BeanConstant.FUTURE_SELL_OPEN)){
                newOrderResponse = spotSyncClientProxy.newOrder(
                        limitBuy(symbol, com.binance.api.client.domain.TimeInForce.GTC,
                                spotQty.toString(),
                                spotPrice.toString()).newOrderRespType(NewOrderResponseType.FULL).newClientOrderId(clientOrderId));
            }else if(direct.equals(BeanConstant.FUTURE_SELL_CLOSE)){
                newOrderResponse = spotSyncClientProxy.newOrder(
                        limitSell(symbol, com.binance.api.client.domain.TimeInForce.GTC,
                                spotQty.toString(),
                                spotPrice.toString()).newOrderRespType(NewOrderResponseType.FULL).newClientOrderId(clientOrderId));
            }


            Long orderId = newOrderResponse.getOrderId();
            log.info("new spot order return,orderid={}", orderId);
////            Thread.sleep(BeanConfig.ORDER_EXPIRE_TIME);
//            if (MarketCache.spotOrderCache.containsKey(orderId) &&
//                    MarketCache.spotOrderCache.get(orderId).getOrderStatus() == OrderStatus.FILLED) {
//                log.info("spot order has been filled, orderId={}", orderId);
//                return;
//            }
            CancelOrderResponse cancelOrderResponse = null;
            //cancel the order
            try {
                cancelOrderResponse = BinanceClient.spotSyncClient.cancelOrder(new CancelOrderRequest(symbol, orderId));
            } catch (Exception exception) {
                if (exception.getMessage().contains("Unknown order sent")) {
                    //order has been filled
                    if(cancelOrderResponse == null){
                        log.info("spot order cancel filled,no need to cancel,symbol={},status={},qty={},orderid={}"
                                ,symbol, newOrderResponse.getStatus(),newOrderResponse.getExecutedQty(),orderId);
                        orderStoreService.processSpotOrder(symbol,clientOrderId,new BigDecimal(newOrderResponse.getPrice())
                                ,new BigDecimal(newOrderResponse.getCummulativeQuoteQty()));
                        return;
                    }else{ //partial filled
                        log.info("spot order cancel partial filled,symbol={},status={},qty={},orderid={}"
                                ,symbol, cancelOrderResponse.getStatus(),cancelOrderResponse.getExecutedQty(),orderId);
                        orderStoreService.processSpotOrder(symbol,clientOrderId,new BigDecimal(newOrderResponse.getPrice())
                                ,new BigDecimal(cancelOrderResponse.getExecutedQty()));
                    }
                }
            }

            if(direct.equals(BeanConstant.FUTURE_SELL_OPEN)){
                spotPrice = MarketCache.spotTickerMap.get(symbol).get(BeanConstant.BEST_ASK_PRICE);
            }else if(direct.equals(BeanConstant.FUTURE_SELL_CLOSE)){
                spotPrice = MarketCache.spotTickerMap.get(symbol).get(BeanConstant.BEST_BID_PRICE);
            }
            spotQty = spotQty.subtract(new BigDecimal(cancelOrderResponse.getExecutedQty()).setScale(spotStepSize, RoundingMode.HALF_UP));
            log.info("spot's order info,spotPrice={}, spotQty={}", spotPrice, spotQty);
        }
    }




}