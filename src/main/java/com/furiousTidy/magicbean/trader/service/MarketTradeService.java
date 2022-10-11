package com.furiousTidy.magicbean.trader.service;


import com.binance.api.client.domain.account.NewOrderResponse;
import com.binance.api.client.domain.account.NewOrderResponseType;
import com.binance.client.model.enums.*;
import com.binance.client.model.trade.Order;
import com.furiousTidy.magicbean.apiproxy.SpotSyncClientProxy;
import com.furiousTidy.magicbean.config.BeanConfig;
import com.furiousTidy.magicbean.dbutil.dao.PairsTradeDao;
import com.furiousTidy.magicbean.dbutil.dao.TradeInfoDao;
import com.furiousTidy.magicbean.util.TradeUtil;
import com.furiousTidy.magicbean.util.BeanConstant;
import com.furiousTidy.magicbean.util.BinanceClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

import static com.binance.api.client.domain.account.NewOrder.*;


/*
* 市价交易策略
* */
@Service
@Slf4j
public class MarketTradeService {

    @Autowired
    SpotSyncClientProxy spotSyncClientProxy;

    @Autowired
    TradeInfoDao tradeInfoDao;

    @Autowired
    PairsTradeDao pairsTradeDao;

    @Autowired
    AfterOrderService orderStoreService;

    @Autowired
    TradeUtil tradeUtil;

    @Autowired
    BinanceClient binanceClient;

    @Async
    public void doFutureTrade(String symbol, BigDecimal futurePrice, BigDecimal futureQty, int futureStepSize,
                              String direct, String clientOrderId) throws InterruptedException{

        OrderSide orderSide =(direct.equals(BeanConstant.FUTURE_SELL_OPEN))? OrderSide.SELL:OrderSide.BUY;
//      PositionSide positionSide = (direct.equals(BeanConstant.FUTURE_SELL_OPEN))?PositionSide.SHORT:PositionSide.LONG;
        PositionSide positionSide = null;
        int i=1;

        while (BeanConstant.watchdog && futureQty.compareTo(BigDecimal.ZERO)>0 && futurePrice.multiply(futureQty).compareTo(BeanConfig.MIN_OPEN_UNIT)>0) {

            log.info("new  future order begin {}, symbol={},orderside={},positionside={},futurePrice={},futureQty={},clientId={}"
                    ,i++,symbol,orderSide,positionSide,futurePrice,futureQty,clientOrderId);
//
            Order order = binanceClient.getFutureSyncClient().postOrder(symbol,orderSide,positionSide, OrderType.MARKET, TimeInForce.IOC,futureQty.toString(),
                    null,null,clientOrderId,null,null, NewOrderRespType.RESULT);

            log.info("futrue new order return: orderid={},status={},qty={},order={}" , clientOrderId,order.getStatus(),order.getExecutedQty(),order);
        }
    }

    @Async
    public void doSpotTrade(String symbol, BigDecimal spotPrice, BigDecimal spotQty, int spotStepSize,String direct,String clientOrderId) throws InterruptedException{
        int i=1;

        while(BeanConstant.watchdog && spotQty.compareTo(BigDecimal.ZERO)>0 &&
                spotPrice.multiply(spotQty).compareTo(BeanConfig.MIN_OPEN_UNIT)>0) {
            NewOrderResponse newOrderResponse = null;
            log.info("new spot order begin {},symbol={},price={},qty={},direct={},clientid={}",i++,symbol,spotPrice,spotQty,direct,clientOrderId);
            if(direct.equals(BeanConstant.FUTURE_SELL_OPEN)){
                newOrderResponse = spotSyncClientProxy.newOrder(
                        marketBuy(symbol,spotQty.toString()).newOrderRespType(NewOrderResponseType.FULL).newClientOrderId(clientOrderId));
            }else if(direct.equals(BeanConstant.FUTURE_SELL_CLOSE)){
                newOrderResponse = spotSyncClientProxy.newOrder(
                        marketSell(symbol, spotQty.toString()).newOrderRespType(NewOrderResponseType.FULL).newClientOrderId(clientOrderId));
            }

            log.info("new spot order return,order={}", newOrderResponse);
        }
    }




}
