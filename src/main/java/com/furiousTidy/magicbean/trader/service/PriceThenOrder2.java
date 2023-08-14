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
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
@Slf4j
public class PriceThenOrder2 {

    public static AtomicBoolean orderFlag = new AtomicBoolean(false);

    @Autowired
    BinanceClient binanceClient;

    @Autowired
    PriceThenOrderHelper priceThenOrderHelper;

    @Autowired
    TradeUtil tradeUtil;

    @Async
    void getPriceInfoThenOrder2() throws InterruptedException {

        log.info("get price inf then order 2");
        int i=0;
        int longCount =0;
        int shortCount =0;

        while(i<3){
            BigDecimal ethPrice1 = (MarketCache.futureTickerMap.get("ETHUSDT")).getBestBidPrice();
            log.info("wss2 price1:{}, timestamp={}",ethPrice1, LocalDateTime.now());
            Thread.sleep(250);

            BigDecimal ethPrice2 = (MarketCache.futureTickerMap.get("ETHUSDT")).getBestBidPrice();
            log.info("wss2 price2:{}, timestamp={}",ethPrice2,LocalDateTime.now());

            if (ethPrice2.compareTo(ethPrice1)>0){
                longCount++;
            }else if(ethPrice2.compareTo(ethPrice1)<0){
                shortCount++;
            }
            i++;
            Thread.sleep(100);
        }
        if(longCount>=1){
            log.info("开始做多");
            priceThenOrderHelper.doNewsStrategyOrder(OrderSide.BUY);
        }else if(shortCount>1){
            log.info("开始做空");
            priceThenOrderHelper.doNewsStrategyOrder(OrderSide.SELL);
        }

        log.info("query 结束");

    }


}
