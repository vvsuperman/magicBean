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
import com.furiousTidy.magicbean.util.BookTickerModel;
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
public class LimitTradeService {

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

    @Autowired
    MarketCache marketCache;



    @Async
    public void doFutureTrade(String symbol, SymbolBookTickerEvent symbolBookTickerEvent, BigDecimal futureQty, int futureStepSize,
                              String direct, String clientOrderId, BigDecimal ratio) throws InterruptedException{

        OrderSide orderSide =(direct.equals(BeanConstant.FUTURE_SELL_OPEN))? OrderSide.SELL:OrderSide.BUY;
        BigDecimal futurePrice = (direct.equals(BeanConstant.FUTURE_SELL_OPEN))? symbolBookTickerEvent.getBestBidPrice():symbolBookTickerEvent.getBestAskPrice();
//      PositionSide positionSide = (direct.equals(BeanConstant.FUTURE_SELL_OPEN))?PositionSide.SHORT:PositionSide.LONG;
        PositionSide positionSide = null;

        if (BeanConstant.watchdog && futureQty.compareTo(BigDecimal.ZERO)>0 && symbolBookTickerEvent.getBestBidPrice().multiply(futureQty).compareTo(BeanConfig.MIN_OPEN_UNIT)>0) {

            if(!BeanConstant.ENOUGH_MONEY.get() && direct.equals(BeanConstant.FUTURE_SELL_OPEN)){
                log.info("future trade detect not enough money,not trade");
                return;
            }

            log.info("new  future order begin:symbol={},orderside={},positionside={},futurePrice={},futureQty={},clientId={},futureBalance={},spotBalance={}"
                    ,symbol,orderSide,positionSide,futurePrice,futureQty,clientOrderId,MarketCache.futureBalance, MarketCache.spotBalance);

            Order order = null;

            try{
                 order = futureSyncClientProxy.postOrder(symbol,orderSide,positionSide, OrderType.LIMIT, TimeInForce.GTC,futureQty.toString(),
                        futurePrice.toString(),null,clientOrderId,null,null, NewOrderRespType.RESULT);

            }catch (Exception e) {
                if (e.getMessage().contains("insufficient")) {
                    log.error("future insufficient money exception......id={}, exception{}", clientOrderId,e);
                    BeanConstant.ENOUGH_MONEY.set(false);
                } else {
                    log.error("future order exception...id={},exception={}",clientOrderId, e);
                }
            }

            log.info("futrue new order return: orderid={},status={},qty={},price={},order={}" , clientOrderId,order.getStatus(),order.getExecutedQty(),order.getAvgPrice(),order);

            // if insufficient, it maybe return null
            if(order == null){
                log.error("future is insufficient null......id={}",clientOrderId);
                BeanConstant.ENOUGH_MONEY.set(false);
                return;
            }
//            if( order.getStatus().equals("FILLED")){
//                BigDecimal price = order.getAvgPrice();
//                BigDecimal qty = order.getExecutedQty();
//                afterOrderService.processFutureOrder(symbol,clientOrderId,price,qty,ratio,symbolBookTickerEvent.getFutureTickDelayTime());
//                return;
//                // order has been partially filled, order status is partially filled, cancel order is null;
//            }
            marketCache.saveFutureOrder(symbol,clientOrderId);
        }
    }

    @Async
    public void doSpotTrade(String symbol, BookTickerModel bookTickerModel, BigDecimal spotQty, int spotStepSize, String direct, String clientOrderId, BigDecimal ratio) throws InterruptedException{
        int i=1;

        if( BeanConstant.watchdog && spotQty.compareTo(BigDecimal.ZERO)>0 &&
                bookTickerModel.getBidPrice().multiply(spotQty).compareTo(BeanConfig.MIN_OPEN_UNIT)>0
        ) {

            if(!BeanConstant.ENOUGH_MONEY.get() && direct.equals(BeanConstant.FUTURE_SELL_OPEN)){
                log.info("spot trade detect not enough money,not trade");
                return;
            }
            BigDecimal spotPrice = (direct.equals(BeanConstant.FUTURE_SELL_OPEN))? bookTickerModel.getAskPrice():bookTickerModel.getBidPrice();


            NewOrderResponse newOrderResponse = null;
            log.info("new spot order begin: symbol={},price={},qty={},direct={},clientid={},futureBalance={},spotBalance={}"
                    ,symbol,spotPrice,spotQty,direct,clientOrderId,MarketCache.futureBalance,MarketCache.spotBalance);

            try{
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

            log.info("new spot order return,clientid={},order={}", newOrderResponse.getClientOrderId(),newOrderResponse);


//            if(newOrderResponse.getStatus() == OrderStatus.FILLED){
//                afterOrderService.processSpotOrder(symbol,clientOrderId,new BigDecimal(newOrderResponse.getFills().get(0).getPrice())
//                        ,new BigDecimal(newOrderResponse.getExecutedQty()),ratio,bookTickerModel.getSpotTickDelayTime());
//
//                return;
//            }
            marketCache.saveSpotOrder(symbol,clientOrderId);
        }
    }


    public static void main(String[] args){
        BigDecimal b = new BigDecimal("1.700000");
        System.out.println(b.setScale(2));
    }




}
