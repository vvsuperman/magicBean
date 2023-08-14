package com.furiousTidy.magicbean.trader.service;

import com.binance.api.client.domain.event.TradeEvent;
import com.furiousTidy.magicbean.config.BeanConfig;
import com.furiousTidy.magicbean.util.BeanConstant;
import com.furiousTidy.magicbean.util.MarketCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;

@Slf4j
@Service
public class MulteExcSpotCompService {

    String maxGapSymbol;
    BigDecimal maxGapRatio = BigDecimal.ZERO;
    BigDecimal maxGapBnPrice;
    LocalDateTime maxGapGapBnTime;
    BigDecimal maxGapMxPrice;
    LocalDateTime maxGapMXTime;

    @Autowired
    RestTemplate restTemplate;

    String fanwanUrl = "https://fwalert.com/e2a2fcbf-80c3-442c-a0a6-64126ed86e9f?message=";

//    @Scheduled(cron = "0 0 0 1/1 * ?")
//    public void clearSendedCache(){
//        log.info("clear sended msg chache");
//
//        MarketCache.sendedMsgSet.clear();
//    }

   // @Scheduled(cron = "0 0/5 * * * ?" )
    public void doCompare(){
        HttpHeaders headers = new HttpHeaders();
        HttpEntity entity = new HttpEntity(headers);
        log.info("do compare........");

        int bnSymbolSize = MarketCache.spotTradeCache.size();
        int mxSymbolSize = MarketCache.mxSpotTradeCache.size();

        for(Map.Entry<String, TradeEvent> bnSpotTrade:MarketCache.spotTradeCache.entrySet()){
            TradeEvent bnTradeEvent = bnSpotTrade.getValue();
            String symbol = bnSpotTrade.getKey();

            if(BeanConfig.EXCLUDE_SYMBOL_LIST.contains(symbol)){
                continue;
            }

            Instant instant = Instant.ofEpochMilli(bnTradeEvent.getTradeTime());
            LocalDateTime bnTradeTime =  LocalDateTime.ofInstant(instant, ZoneId.systemDefault());


            if( MarketCache.mxSpotTradeCache.containsKey(symbol)){

                TradeEvent mxTradeEvent = MarketCache.mxSpotTradeCache.get(symbol);
                BigDecimal bnPrice = bnTradeEvent.getPrice();
                BigDecimal mxPrice = mxTradeEvent.getPrice();

                Instant mxInstant = Instant.ofEpochMilli(mxTradeEvent.getTradeTime());
                LocalDateTime mxTradeTime =  LocalDateTime.ofInstant(mxInstant, ZoneId.systemDefault());
                BigDecimal gapRatio = bnPrice.subtract(mxPrice).divide(bnPrice,4, RoundingMode.UP).abs();
                if(gapRatio.compareTo(BigDecimal.valueOf(BeanConfig.MULTI_EXCHANGE_SPOT_GAP))>0 &&
                      Math.abs(Duration.between(bnTradeTime,mxTradeTime).toMinutes())<1){
                    String msg = symbol+ "超过价差:"+ gapRatio.multiply(BigDecimal.valueOf(100)) +"%  bn价格:" + bnPrice +  " mx价格:"+ mxPrice + " bn交易时间:"+ bnTradeTime +
                            " mx交易时间:"+ mxTradeTime +" 监测bn币对:"+bnSymbolSize +" 监测mx币对:"+ mxSymbolSize;

                        log.info("send msg" + msg);
                        restTemplate.getForEntity(fanwanUrl+msg,String.class);



                }

                if(gapRatio.compareTo(maxGapRatio)>0){
                    maxGapRatio = gapRatio;
                    maxGapSymbol = symbol;
                    maxGapBnPrice = bnPrice;
                    maxGapGapBnTime = bnTradeTime;
                    maxGapMxPrice = mxPrice;
                    maxGapMXTime = mxTradeTime;
                }

            }
        }
    }


//    @Scheduled(cron = "* * 8,20 * * ?" )
//    public void alertCompare(){
//        String msg = "最大价差为:" + maxGapSymbol + "价差:"+ maxGapRatio + " bn价格："+ maxGapBnPrice +" bn时间:"+ maxGapGapBnTime + " mx价格:" + maxGapMxPrice + " mx时间:" + maxGapMXTime;
//
//        restTemplate.getForEntity(fanwanUrl+msg,String.class);
//        log.info(msg);
//
//    }

}
