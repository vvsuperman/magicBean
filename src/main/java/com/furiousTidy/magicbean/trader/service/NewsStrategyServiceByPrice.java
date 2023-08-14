package com.furiousTidy.magicbean.trader.service;


import com.binance.client.model.enums.NewOrderRespType;
import com.binance.client.model.enums.OrderSide;
import com.binance.client.model.enums.OrderType;
import com.binance.client.model.event.SymbolBookTickerEvent;
import com.binance.client.model.trade.Order;
import com.furiousTidy.magicbean.config.BeanConfig;
import com.furiousTidy.magicbean.trader.TradeDto.JinShiDto;
import com.furiousTidy.magicbean.util.BeanConstant;
import com.furiousTidy.magicbean.util.BinanceClient;
import com.furiousTidy.magicbean.util.MarketCache;
import com.furiousTidy.magicbean.util.TradeUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

//新闻流套利
@Component
@Slf4j
public class NewsStrategyServiceByPrice {

    List<String> cpiList = Arrays.asList("2022-11-10 08:30","2022-11-23 21:30","2022-12-13 21:30","2023-01-12 21:30","2023-02-14 21:30","2023-03-14 08:30",
            "2023-04-14 08:30","2023-05-10 08:30","2023-06-10 08:30","2023-07-12 08:30","2023-08-10 08:30",
            "2023-06-10 08:30","2023-08-10 08:30","2023-09-13 08:30","2023-10-12 08:30","2023-11-12 08:30",
            "2023-12-12 08:30");

    //public static String NEWS_TIME = "2022-12-15 03:00";
    public static String NEWS_TIME = "2023-01-12 21:30";
    @Autowired
    BinanceClient binanceClient;

    @Autowired
    PriceThenOrder2 priceThenOrder2;

    @Autowired
    PriceThenOrder3 priceThenOrder3;


    @Autowired
    TradeUtil tradeUtil;

    @Autowired
    RestTemplate restTemplate;

    @Autowired
    PriceThenOrderHelper priceThenOrderHelper;


    static final DateTimeFormatter formatter1 = DateTimeFormatter.ofPattern("yyyy/MMdd");
    static final DateTimeFormatter formatter2 = DateTimeFormatter.ofPattern("yyyyMMddHH");
    static final DateTimeFormatter formatter3 = DateTimeFormatter.ofPattern("mm");
    static final DateTimeFormatter formatter4 = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Async
    public void doNewsStrategy() throws InterruptedException {
        AtomicReference<Float> blCpi= new AtomicReference<Float>();
        LocalDateTime newsTime =  LocalDateTime.parse(NEWS_TIME, formatter4);

        while(true) {
            //大于结束时间，结束
            if (LocalDateTime.now().isAfter(newsTime)) {
                log.info("策略执行开始,{}", LocalDateTime.now());
                //策略开始执行，获取新闻流的值，判断并下单
                getPriceInfoThenOrder();
//                priceThenOrder2.getPriceInfoThenOrder2();
//                priceThenOrder3.getPriceInfoThenOrder2();
                log.info("策略执行完毕，{}", LocalDateTime.now());
                return;
            }else{
                log.info("策略执行时间未开始,{}", LocalDateTime.now());
                Thread.sleep(100);
            }
        }
    }



    void getPriceInfoThenOrder() throws InterruptedException {
        int i=0;
        int longCount =0;
        int shortCount =0;

        while(i<3){
            Thread.sleep(50);

            BigDecimal ethPrice1 = (MarketCache.futureTickerMap.get("ETHUSDT")).getBestBidPrice();
            log.info("ssw price1:{}, timestamp={}",ethPrice1,LocalDateTime.now());
            Thread.sleep(50);

            BigDecimal ethPrice2 = (MarketCache.futureTickerMap.get("ETHUSDT")).getBestBidPrice();
            log.info("ssw price2:{}, timestamp={}",ethPrice2,LocalDateTime.now());

            if (ethPrice2.compareTo(ethPrice1)>0){
                longCount++;
            }else if(ethPrice2.compareTo(ethPrice1)<0){
                shortCount++;
            }
            i++;
        }
        if(longCount>=1){
            log.info("开始做多");
            priceThenOrderHelper.doNewsStrategyOrder(OrderSide.BUY);
        }else if(shortCount>1){
            log.info("开始做空");
            priceThenOrderHelper.doNewsStrategyOrder(OrderSide.SELL);
        }
        log.info("wss 结束");
    }









    public static void main(String[] args){

        NewsStrategyServiceByPrice newsStrategyService = new NewsStrategyServiceByPrice();
       // LocalDateTime newsTime =  LocalDateTime.parse(BeanConfig.NEWS_TIME, formatter4);

    }

}
