package com.furiousTidy.magicbean.trader.service;


import com.binance.client.model.enums.NewOrderRespType;
import com.binance.client.model.enums.OrderSide;
import com.binance.client.model.enums.OrderType;
import com.binance.client.model.trade.Order;
import com.furiousTidy.magicbean.config.BeanConfig;
import com.furiousTidy.magicbean.trader.TradeDto.JinShiDto;
import com.furiousTidy.magicbean.util.TradeUtil;
import com.furiousTidy.magicbean.util.BeanConstant;
import com.furiousTidy.magicbean.util.BinanceClient;
import com.furiousTidy.magicbean.util.MarketCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

//新闻流套利
@Component
@Slf4j
public class NewsStrategyService {

    @Autowired
    BinanceClient binanceClient;

    @Autowired
    TradeUtil tradeUtil;

    @Autowired
    RestTemplate restTemplate;

    static final DateTimeFormatter formatter1 = DateTimeFormatter.ofPattern("yyyy/MMdd");
    static final DateTimeFormatter formatter2 = DateTimeFormatter.ofPattern("yyyyMMddHH");
    static final DateTimeFormatter formatter3 = DateTimeFormatter.ofPattern("mm");

    static final DateTimeFormatter formatter4 = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");


    public void doNewsStrategy() throws InterruptedException {


        LocalDateTime newsTime =  LocalDateTime.parse(BeanConfig.NEWS_TIME, formatter4);
        LocalDateTime newsTimeBegin = newsTime.minusSeconds(BeanConfig.NEWS_STARTTIME_GAP );
        LocalDateTime newsTimeEnd = newsTime.plusSeconds(BeanConfig.NEWS_ENDTIME_GAP );

        log.info("newsTimeBegin={}, newsTimeEnd={}", newsTimeBegin, newsTimeEnd);

        while(true){
            if(BeanConfig.NEW_STOP_FLAG){
                return;
            }
            //小于开始时间
            if(LocalDateTime.now().isBefore(newsTimeBegin)){
                Thread.sleep( BeanConfig.NEWS_INTEVL );
                log.info("策略尚未开始，{}", LocalDateTime.now());
                continue;
            }
            //大于结束时间，结束
            if(LocalDateTime.now().isAfter(newsTimeEnd))
            {
                log.info("策略结束时间已到,{}", LocalDateTime.now());
                return;
            }

            log.info("策略执行开始,{}", LocalDateTime.now());

            //策略开始执行，获取新闻流的值，判断并下单
            if(getInfoThenOrder()){
                log.info("策略执行完毕，{}", LocalDateTime.now());
                return;
            }

            //get请求延时 500ms-1s 加个小延时
            Thread.sleep(10);
        }

    }

    public boolean getInfoThenOrder() {

        List<JinShiDto> jinShiList;
        jinShiList = getJinShiDtoList();


        for (JinShiDto jinShiDto : jinShiList) {//新闻流策略
            if (jinShiDto.getName().contains(BeanConfig.NEWS_NAME) && jinShiDto.getActual() != null && jinShiDto.getConsensus() != null) {
                log.info("test 策略开始.......");
                //实际值
                float actual = Float.parseFloat(jinShiDto.getActual());
                //预测值
                float consensus = Float.parseFloat(jinShiDto.getConsensus());
                float offset = (actual - consensus) / consensus;

                OrderSide orderSide = null;
                //加息超过预期，做空
                if (offset > 0 && offset > BeanConfig.NEWS_THREHOLD ) {
                    log.info(BeanConfig.NEWS_NAME + "指标超过预期，做空" + jinShiDto);
                    doNewsStrategyOrder(OrderSide.SELL);
                }
                //加息小于预期，做多
                else if (offset < 0 && Math.abs(offset) >  BeanConfig.NEWS_THREHOLD ) {
                    log.info(BeanConfig.NEWS_NAME + "指标小于预期，做多" + jinShiDto);
                    doNewsStrategyOrder(OrderSide.BUY);
                } else {
                    log.info(BeanConfig.NEWS_NAME + "指标=预期" + jinShiDto);
                }
                return true;
            }

            //美联储加息利率决定
            if (jinShiDto.getName().contains(BeanConstant.FED) && jinShiDto.getActual() != null && jinShiDto.getConsensus() != null) {
                log.info("加息策略开始执行......." + LocalDateTime.now());

                //实际加息值
                float actual = Float.parseFloat(jinShiDto.getActual());
                //预测加息值
                float consensus = Float.parseFloat(jinShiDto.getConsensus());
                float offset = (actual - consensus) / consensus;

                OrderSide orderSide = null;
                //加息超过预期，做空
                if (offset > 0 && offset > BeanConstant.FED_THRESHOLD) {
                    log.info("加息超过预期" + jinShiDto);
                    orderSide = OrderSide.SELL;
                    doNewsStrategyOrder(orderSide);
                }
                //加息小于预期，做多
                else if (offset < 0 && Math.abs(offset) > BeanConstant.FED_THRESHOLD) {
                    log.info("加息小于预期" + jinShiDto);
                    orderSide = OrderSide.BUY;
                    doNewsStrategyOrder(orderSide);
                } else {
                    log.info("加息=预期" + jinShiDto);

                }
                return true;
            }
            //CPI年率
            if (jinShiDto.getName().contains(BeanConstant.CPI) && jinShiDto.getActual() != null && jinShiDto.getConsensus() != null) {

                log.info("cpi策略开始执行.......");

                float offset = Float.parseFloat(jinShiDto.getActual()) - Float.parseFloat(jinShiDto.getConsensus());
                OrderSide orderSide = null;
                //CPI超过预期，做空
                if (offset > BeanConstant.CPI_THRESHOLD) {
                    log.info("cpi超过预期" + jinShiDto);
                    orderSide = OrderSide.SELL;
                    doNewsStrategyOrder(orderSide);

                }
                //CPI小于预期，做多
                else if (offset < 0 && Math.abs(offset) > BeanConstant.CPI_THRESHOLD) {
                    log.info("cpi小于预期" + jinShiDto);
                    orderSide = OrderSide.BUY;
                    doNewsStrategyOrder(orderSide);
                } else {
                    log.info("加息=预期" + jinShiDto);
                }
                return true;
            }
        }

        return false;
    }

    public List<JinShiDto> getJinShiDtoList() {
        List<JinShiDto> jinShiList = null;
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/json");
        HttpEntity entity = new HttpEntity(headers);
        ParameterizedTypeReference<List<JinShiDto>> responseType = new ParameterizedTypeReference<List<JinShiDto>>() {};


        String url = generateUrl();

        log.info("get jinshi data begin: " + LocalDateTime.now()+"url="+url);

        try{
            jinShiList = restTemplate.exchange(url, HttpMethod.GET,entity, responseType).getBody();
        }catch (Exception ex){
            log.error("get url exception: {}", ex);
        }

        log.info("get jinshi data finished: " + LocalDateTime.now());

        return jinShiList;
    }

    //下单，开合约！
    public void doNewsStrategyOrder(OrderSide orderSide) {

        String symbol = "ETHUSDT";

        Integer[] stepSize = tradeUtil.getStepSize(symbol);
        BigDecimal futureQuantity =
          MarketCache.futureBalance.get()
                  .divide(MarketCache.futureTickerMap.get(symbol).getBestBidPrice(), stepSize[0], BigDecimal.ROUND_HALF_UP)
                  .multiply(BigDecimal.valueOf(BeanConfig.LEV));

        log.info("order params: futurebalance={},futureQuantity={} ", MarketCache.futureBalance.get(), futureQuantity);
        Order order = binanceClient.getFutureSyncClient().postOrder(symbol, orderSide,null, OrderType.MARKET, null ,futureQuantity.toString(),
                null,null,null,null,null, NewOrderRespType.RESULT);

        log.info("news Strategy order return: orderid={},status={},qty={},order={}" , order.getOrderId(),order.getStatus(),order.getExecutedQty(),order);
    }

    //https://cdn-rili.jin10.com/data/2022/0728/economics.json?_=202207281603&date=2022-07-28T08:23:43.290Z
    String generateUrl(){
        LocalDateTime localDateTime = LocalDateTime.now();

        String dateTime1 = localDateTime.format(formatter1);
        String dateTime2 = localDateTime.format(formatter2);
        String minute = localDateTime.format(formatter3);

        String pm = String.valueOf(Integer.parseInt(minute.substring(0,1))+1);

        String url = String.valueOf(new StringBuilder().append(BeanConstant.JINSHIURL)
                .append(dateTime1).append(BeanConstant.JINSHIURL_SUB1)
                .append(dateTime2).append(BeanConstant.JINSHIURL_SUB3)
                .append(pm).append(BeanConstant.JINSHIURL_SUB2)
                .append(OffsetDateTime.now(ZoneOffset.UTC)));
        log.info("general URL = {}", url);

        return url;
    }

    public static void main(String[] args){

        System.out.println( new NewsStrategyService().generateUrl());

       // LocalDateTime newsTime =  LocalDateTime.parse(BeanConfig.NEWS_TIME, formatter4);



    }
}
