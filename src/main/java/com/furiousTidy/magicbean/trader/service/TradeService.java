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
import com.furiousTidy.magicbean.trader.controller.PositionOpenController;
import com.furiousTidy.magicbean.util.BeanConstant;
import com.furiousTidy.magicbean.util.BinanceClient;
import com.furiousTidy.magicbean.util.MarketCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.atomic.AtomicBoolean;
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

    @Autowired
    TradeUtil tradeUtil;



    @Async
    public void doFutureTrade(String symbol, BigDecimal futurePrice, BigDecimal futureQty, int futureStepSize,
                              String direct, String clientOrderId) throws InterruptedException{

        OrderSide orderSide =(direct.equals(BeanConstant.FUTURE_SELL_OPEN))? OrderSide.SELL:OrderSide.BUY;
//      PositionSide positionSide = (direct.equals(BeanConstant.FUTURE_SELL_OPEN))?PositionSide.SHORT:PositionSide.LONG;
        PositionSide positionSide = null;
        int i=1;

        while ( PositionOpenController.watchdog && futureQty.compareTo(BigDecimal.ZERO)>0 && futurePrice.multiply(futureQty).compareTo(BeanConfig.MIN_OPEN_UNIT)>0) {

            log.info("new  future order begin {}, symbol={},orderside={},positionside={},futurePrice={},futureQty={},clientId={}"
                    ,i++,symbol,orderSide,positionSide,futurePrice,futureQty,clientOrderId);
            Order order = BinanceClient.futureSyncClient.postOrder(symbol,orderSide,positionSide, OrderType.LIMIT, TimeInForce.IOC,futureQty.toString(),
                    futurePrice.toString(),null,clientOrderId,null,null, NewOrderRespType.RESULT);
            log.info("futrue new order return: orderid={},status={},qty={}" , clientOrderId,order.getStatus(),order.getExecutedQty());

            if( order.getStatus().equals("FILLED")){
                orderStoreService.processFutureOrder(clientOrderId,order);
                if(direct.equals(BeanConstant.FUTURE_SELL_CLOSE)){
                    BeanConstant.ENOUGH_MONEY.set(true);
                }
                return;
                // order has been partially filled, order status is partially filled, cancel order is null;
            }else if(order.getStatus().equals("PARTIALLY_FILLED" )){
                orderStoreService.processFutureOrder(clientOrderId,order);
                futureQty = futureQty.subtract(order.getExecutedQty().setScale(futureStepSize, RoundingMode.HALF_UP));
            }else if(order.getStatus().equals("EXPIRED") && order.getExecutedQty().compareTo(BigDecimal.ZERO)>0){
                orderStoreService.processFutureOrder(clientOrderId,order);
                futureQty = futureQty.subtract(order.getExecutedQty().setScale(futureStepSize, RoundingMode.HALF_UP));
            }

            if(direct.equals(BeanConstant.FUTURE_SELL_OPEN)){
                futurePrice =  MarketCache.futureTickerMap.get(symbol).get(BeanConstant.BEST_BID_PRICE);
            }else if(direct.equals(BeanConstant.FUTURE_SELL_CLOSE)){
                futurePrice = MarketCache.futureTickerMap.get(symbol).get(BeanConstant.BEST_ASK_PRICE);
            }

            log.info("future's next order info,bidPrice={}, futureQty={}",futurePrice,futureQty);
        }
    }

    @Async
    public void doSpotTrade(String symbol, BigDecimal spotPrice, BigDecimal spotQty, int spotStepSize,String direct,String clientOrderId) throws InterruptedException{
        int i=1;

        while( PositionOpenController.watchdog && spotQty.compareTo(BigDecimal.ZERO)>0 &&
                spotPrice.multiply(spotQty).compareTo(BeanConfig.MIN_OPEN_UNIT)>0) {
            NewOrderResponse newOrderResponse = null;
            log.info("new spot order begin {},symbol={},price={},qty={},direct={},clientid={}",i++,symbol,spotPrice,spotQty,direct,clientOrderId);

            try{
                if(direct.equals(BeanConstant.FUTURE_SELL_OPEN)){
                    newOrderResponse = spotSyncClientProxy.newOrder(
                            limitBuy(symbol, com.binance.api.client.domain.TimeInForce.IOC,
                                    spotQty.toString(),
                                    spotPrice.toString()).newOrderRespType(NewOrderResponseType.FULL).newClientOrderId(clientOrderId));
                }else if(direct.equals(BeanConstant.FUTURE_SELL_CLOSE)){
                    newOrderResponse = spotSyncClientProxy.newOrder(
                            limitSell(symbol, com.binance.api.client.domain.TimeInForce.IOC,
                                    spotQty.toString(),
                                    spotPrice.toString()).newOrderRespType(NewOrderResponseType.FULL).newClientOrderId(clientOrderId));
                }
            }catch (Exception e){
                if(e.getMessage().contains("insufficient balance")){
                    log.error("insufficient money......exception{}",e);
                    BeanConstant.ENOUGH_MONEY.set(false);
                }else{
                    log.error("spot order exception={}",e);
                }
            }

            log.info("new spot order return,order={}", newOrderResponse);

            // if insufficient, it maybe return null
            if(newOrderResponse == null){
                BeanConstant.ENOUGH_MONEY.set(false);
                return;
            }

            if(newOrderResponse.getStatus() == OrderStatus.FILLED){
                orderStoreService.processSpotOrder(symbol,clientOrderId,spotPrice,spotQty);
                if(direct.equals(BeanConstant.FUTURE_SELL_CLOSE)){
                    BeanConstant.ENOUGH_MONEY.set(true);
                }
                return;
            }else if(newOrderResponse.getStatus() == OrderStatus.PARTIALLY_FILLED ){
                orderStoreService.processSpotOrder(symbol,clientOrderId,spotPrice,new BigDecimal(newOrderResponse.getExecutedQty()));
                spotQty = spotQty.subtract(new BigDecimal(newOrderResponse.getExecutedQty()).setScale(spotStepSize, RoundingMode.HALF_UP));
            }else if(newOrderResponse.getStatus() == OrderStatus.EXPIRED && new BigDecimal(newOrderResponse.getExecutedQty()).compareTo(BigDecimal.ZERO)>0){
                orderStoreService.processSpotOrder(symbol,clientOrderId,spotPrice,new BigDecimal(newOrderResponse.getExecutedQty()));
                spotQty = spotQty.subtract(new BigDecimal(newOrderResponse.getExecutedQty()).setScale(spotStepSize, RoundingMode.HALF_UP));
            }

            if(direct.equals(BeanConstant.FUTURE_SELL_OPEN)){
                spotPrice = MarketCache.spotTickerMap.get(symbol).get(BeanConstant.BEST_ASK_PRICE);
            }else if(direct.equals(BeanConstant.FUTURE_SELL_CLOSE)){
                spotPrice = MarketCache.spotTickerMap.get(symbol).get(BeanConstant.BEST_BID_PRICE);
            }

            log.info("spot's next order info,spotPrice={}, spotQty={}", spotPrice, spotQty);
        }
    }


    public static void main(String[] args){
        BigDecimal b = new BigDecimal("1.700000");
        System.out.println(b.setScale(2));
    }




}
