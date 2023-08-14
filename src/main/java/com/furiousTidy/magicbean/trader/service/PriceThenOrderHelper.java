package com.furiousTidy.magicbean.trader.service;

import com.binance.client.model.enums.NewOrderRespType;
import com.binance.client.model.enums.OrderSide;
import com.binance.client.model.enums.OrderType;
import com.binance.client.model.trade.Order;
import com.furiousTidy.magicbean.config.BeanConfig;
import com.furiousTidy.magicbean.util.BinanceClient;
import com.furiousTidy.magicbean.util.MarketCache;
import com.furiousTidy.magicbean.util.TradeUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
@Slf4j
public class PriceThenOrderHelper {

    public static AtomicBoolean orderFlag = new AtomicBoolean(false);

    @Autowired
    BinanceClient binanceClient;

    @Autowired
    TradeUtil tradeUtil;



    public void doNewsStrategyOrder(OrderSide orderSide) {

        if (PriceThenOrderHelper.orderFlag.equals(new AtomicBoolean(true))){
            return;
        }

        PriceThenOrderHelper.orderFlag.getAndSet(true);

        String symbol = "ETHUSDT";

        Integer[] stepSize = tradeUtil.getStepSize(symbol);
        BigDecimal futureQuantity =
                MarketCache.futureBalance.get()
                        .divide(MarketCache.futureTickerMap.get(symbol).getBestBidPrice(), stepSize[0], RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(BeanConfig.LEV));

        log.info("order params: futurebalance={},futureQuantity={} ", MarketCache.futureBalance.get(), futureQuantity);
        Order order = binanceClient.getFutureSyncClient().postOrder(symbol, orderSide,null, OrderType.MARKET, null ,futureQuantity.toString(),
                null,null,null,null,null, NewOrderRespType.RESULT);

        log.info("news Strategy order return: orderid={},status={},qty={},order={}" , order.getOrderId(),order.getStatus(),order.getExecutedQty(),order);
    }
}
