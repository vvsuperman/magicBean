package com.furiousTidy.magicbean.apiproxy;

import com.binance.client.model.enums.*;
import com.binance.client.model.market.ExchangeInformation;
import com.binance.client.model.market.SymbolOrderBook;
import com.binance.client.model.trade.Order;
import com.furiousTidy.magicbean.config.BeanConfig;
import com.furiousTidy.magicbean.util.BeanConstant;
import com.furiousTidy.magicbean.util.BinanceClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class FutureSyncClientProxy {

    @Autowired
    ProxyUtil  proxyUtil;

    @Retryable( maxAttempts = 5, backoff = @Backoff(delay = 2000, multiplier = 1.5))
    public ExchangeInformation getExchangeInfo(){
        log.info("try to get spot exchangeinfo");
        return BinanceClient.futureSyncClient.getExchangeInformation();
    }

    @Retryable( maxAttempts = 5, backoff = @Backoff(delay = 2000, multiplier = 1.5))
    public List<SymbolOrderBook> getAllBookTickers(){
        return BinanceClient.futureSyncClient.getSymbolOrderBookTicker(null);
    }

    public Order postOrder(String var1, OrderSide var2, PositionSide var3, OrderType var4, TimeInForce var5, String var6, String var7, String var8, String var9, String var10, WorkingType var11, NewOrderRespType var12){
        long start = System.currentTimeMillis();
        Order order = BinanceClient.futureSyncClient.postOrder(var1,var2,var3,var4,var5,var6,var7,var8,var9,var10,var11,var12);

        // adjust balance
        proxyUtil.changeBalance(BeanConfig.STANDARD_TRADE_UNIT.subtract(order.getPrice().multiply(order.getExecutedQty())),"future");
        //
        long duration = System.currentTimeMillis() - start;
        if(duration > 50){
            log.info("future network is too slow, stop trade, duration={}",duration);
            BeanConstant.NETWORK_DELAYED = true;
            new Thread(() -> {
                try {
                    Thread.sleep(BeanConfig.NET_DELAY_TIME);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    BeanConstant.NETWORK_DELAYED = false;
                }

            }).start();
        }
        return order;
    };

}
