package com.furiousTidy.magicbean.trader.service;


import com.binance.api.client.domain.OrderStatus;
import com.binance.api.client.domain.account.NewOrderResponse;
import com.binance.api.client.domain.account.NewOrderResponseType;
import com.binance.client.model.enums.*;
import com.binance.client.model.event.SymbolBookTickerEvent;
import com.binance.client.model.trade.Order;
import com.furiousTidy.magicbean.apiproxy.FutureSyncClientProxy;
import com.furiousTidy.magicbean.apiproxy.ProxyUtil;
import com.furiousTidy.magicbean.apiproxy.SpotSyncClientProxy;
import com.furiousTidy.magicbean.config.BeanConfig;
import com.furiousTidy.magicbean.dbutil.dao.PairsTradeDao;
import com.furiousTidy.magicbean.dbutil.dao.TradeInfoDao;
import com.furiousTidy.magicbean.trader.TradeUtil;
import com.furiousTidy.magicbean.util.BeanConstant;
import com.furiousTidy.magicbean.util.MarketCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static com.binance.api.client.domain.account.NewOrder.*;

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
    AfterOrderService afterOrderService;

    @Autowired
    TradeUtil tradeUtil;

    @Autowired
    FutureSyncClientProxy futureSyncClientProxy;

    @Autowired
    ProxyUtil proxyUtil;



    @Async
    public void doFutureTrade(String symbol, SymbolBookTickerEvent symbolBookTickerEvent, BigDecimal futureQty, int futureStepSize,
                              String direct, String clientOrderId, BigDecimal ratio) throws InterruptedException{

        OrderSide orderSide =(direct.equals(BeanConstant.FUTURE_SELL_OPEN))? OrderSide.SELL:OrderSide.BUY;
        BigDecimal futurePrice = (direct.equals(BeanConstant.FUTURE_SELL_OPEN))? symbolBookTickerEvent.getBestBidPrice():symbolBookTickerEvent.getBestAskPrice();
//      PositionSide positionSide = (direct.equals(BeanConstant.FUTURE_SELL_OPEN))?PositionSide.SHORT:PositionSide.LONG;
        PositionSide positionSide = null;
        int i=1;

        while (BeanConstant.watchdog && futureQty.compareTo(BigDecimal.ZERO)>0 && symbolBookTickerEvent.getBestBidPrice().multiply(futureQty).compareTo(BeanConfig.MIN_OPEN_UNIT)>0) {

            if(!BeanConstant.ENOUGH_MONEY.get() && direct.equals(BeanConstant.FUTURE_SELL_OPEN)){
                log.info("future trade detect not enough money,not trade");
                return;
            }

            log.info("new  future order begin {}, symbol={},orderside={},positionside={},futurePrice={},futureQty={},clientId={},futureBalance={},spotBalance={}"
                    ,i++,symbol,orderSide,positionSide,futurePrice,futureQty,clientOrderId,MarketCache.futureBalance, MarketCache.spotBalance);

            Order order = null;

            try{
//                 order = binanceClient.getFutureSyncClient().postOrder(symbol,orderSide,positionSide, OrderType.LIMIT, TimeInForce.IOC,futureQty.toString(),
//                        futurePrice.toString(),null,clientOrderId,null,null, NewOrderRespType.RESULT);
                order = futureSyncClientProxy.postOrder(symbol,orderSide,positionSide, OrderType.MARKET, null,futureQty.toString(),
                        null,null,clientOrderId,null,null, NewOrderRespType.RESULT);
            }catch (Exception e) {
                if (e.getMessage().contains("insufficient")) {
                    log.error("future insufficient money exception......id={}, exception{}", clientOrderId,e);
                    BeanConstant.ENOUGH_MONEY.set(false);
                } else {
                    log.error("future order exception...id={},exception={}",clientOrderId, e);
                }
            }

            // if insufficient, it maybe return null
            if(order == null){
                log.error("future is insufficient null......id={}",clientOrderId);
                BeanConstant.ENOUGH_MONEY.set(false);
                return;
            }


            log.info("futrue new order return: orderid={},status={},qty={},price={},order={}" , clientOrderId,order.getStatus(),order.getExecutedQty(),order.getAvgPrice(),order);


            BigDecimal price = order.getAvgPrice();
            BigDecimal qty = order.getExecutedQty();

            if( order.getStatus().equals("FILLED")){

                afterOrderService.processFutureOrder(symbol,clientOrderId,price,qty,ratio,symbolBookTickerEvent.getFutureTickDelayTime());

                return;
                // order has been partially filled, order status is partially filled, cancel order is null;
            }else if(order.getStatus().equals("PARTIALLY_FILLED" )){
                afterOrderService.processFutureOrder(symbol,clientOrderId,price,qty,ratio,symbolBookTickerEvent.getFutureTickDelayTime());
                futureQty = futureQty.subtract(order.getExecutedQty().setScale(futureStepSize, RoundingMode.HALF_UP));
            }else if(order.getStatus().equals("EXPIRED") && order.getExecutedQty().compareTo(BigDecimal.ZERO)>0){
                afterOrderService.processFutureOrder(symbol,clientOrderId,price,qty,ratio,symbolBookTickerEvent.getFutureTickDelayTime());
                futureQty = futureQty.subtract(order.getExecutedQty().setScale(futureStepSize, RoundingMode.HALF_UP));
            }

//            if(direct.equals(BeanConstant.FUTURE_SELL_OPEN)){
//                futurePrice =  MarketCache.futureTickerMap.get(symbol).get(BeanConstant.BEST_BID_PRICE);
//            }else if(direct.equals(BeanConstant.FUTURE_SELL_CLOSE)){
//                futurePrice = MarketCache.futureTickerMap.get(symbol).get(BeanConstant.BEST_ASK_PRICE);
//            }

            log.info("future's next order info,bidPrice={}, futureQty={}",futurePrice,futureQty);
        }
    }

    @Async
    public void doSpotTrade(String symbol, BigDecimal spotPrice, BigDecimal spotQty, int spotStepSize,String direct,String clientOrderId, BigDecimal ratio) throws InterruptedException{
        int i=1;

        while( BeanConstant.watchdog && spotQty.compareTo(BigDecimal.ZERO)>0 &&
                spotPrice.multiply(spotQty).compareTo(BeanConfig.MIN_OPEN_UNIT)>0) {

            if(!BeanConstant.ENOUGH_MONEY.get() && direct.equals(BeanConstant.FUTURE_SELL_OPEN)){
                log.info("spot trade detect not enough money,not trade");
                return;
            }

            NewOrderResponse newOrderResponse = null;
            log.info("new spot order begin {},symbol={},price={},qty={},direct={},clientid={},futureBalance={},spotBalance={}"
                    ,i++,symbol,spotPrice,spotQty,direct,clientOrderId,MarketCache.futureBalance,MarketCache.spotBalance);

            try{
                if(direct.equals(BeanConstant.FUTURE_SELL_OPEN)){
//                    newOrderResponse = spotSyncClientProxy.newOrder(
//                            limitBuy(symbol, com.binance.api.client.domain.TimeInForce.IOC,
//                                    spotQty.toString(),
//                                    spotPrice.toString()).newOrderRespType(NewOrderResponseType.FULL).newClientOrderId(clientOrderId));
                    newOrderResponse = spotSyncClientProxy.newOrder(
                            marketBuy(symbol, spotQty.toString()).newOrderRespType(NewOrderResponseType.FULL).newClientOrderId(clientOrderId));
                }else if(direct.equals(BeanConstant.FUTURE_SELL_CLOSE)){
//                    newOrderResponse = spotSyncClientProxy.newOrder(
//                            limitSell(symbol, com.binance.api.client.domain.TimeInForce.IOC,
//                                    spotQty.toString(),
//                                    spotPrice.toString()).newOrderRespType(NewOrderResponseType.FULL).newClientOrderId(clientOrderId));
                    newOrderResponse = spotSyncClientProxy.newOrder(
                            marketSell(symbol,spotQty.toString()).newOrderRespType(NewOrderResponseType.FULL).newClientOrderId(clientOrderId));
                }
            }catch (Exception e){
                if(e.getMessage().contains("insufficient balance")){
                    log.error("spot insufficient money exception......id={},exception={}",clientOrderId,e);
                    BeanConstant.ENOUGH_MONEY.set(false);
                }else{
                    log.error("spot order exception...id={},exception={}",clientOrderId,e);
                }
            }

            // if insufficient money, it maybe return null
            if(newOrderResponse == null){
                log.error("spot is insufficient null......id={}",clientOrderId);
                BeanConstant.ENOUGH_MONEY.set(false);
                return;
            }

            log.info("new spot order return,clientid={},price={},qty={},order={}", newOrderResponse.getClientOrderId()
                    ,newOrderResponse.getFills().get(0).getPrice(),newOrderResponse.getExecutedQty(),newOrderResponse);


            if(newOrderResponse.getStatus() == OrderStatus.FILLED){


                afterOrderService.processSpotOrder(symbol,clientOrderId,new BigDecimal(newOrderResponse.getFills().get(0).getPrice())
                        ,new BigDecimal(newOrderResponse.getExecutedQty()),ratio);

                return;
            }else if(newOrderResponse.getStatus() == OrderStatus.PARTIALLY_FILLED ){
                afterOrderService.processSpotOrder(symbol,clientOrderId,spotPrice,new BigDecimal(newOrderResponse.getExecutedQty()),ratio);
                spotQty = spotQty.subtract(new BigDecimal(newOrderResponse.getExecutedQty()).setScale(spotStepSize, RoundingMode.HALF_UP));
            }else if(newOrderResponse.getStatus() == OrderStatus.EXPIRED && new BigDecimal(newOrderResponse.getExecutedQty()).compareTo(BigDecimal.ZERO)>0){
                afterOrderService.processSpotOrder(symbol,clientOrderId,spotPrice,new BigDecimal(newOrderResponse.getExecutedQty()),ratio);
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
