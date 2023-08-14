package com.furiousTidy.magicbean.trader.service;


import com.binance.api.client.domain.event.TradeEvent;
import com.furiousTidy.magicbean.config.BeanConfig;
import com.furiousTidy.magicbean.trader.TradeDto.MxTickerEvent;
import com.furiousTidy.magicbean.util.MarketCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
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

@Service
@Slf4j
public class SpotPerpGapService {

    @Autowired
    RestTemplate restTemplate;

    String fanwanUrl = "https://fwalert.com/e2a2fcbf-80c3-442c-a0a6-64126ed86e9f?message=";

    @Scheduled(cron = "0/10 * * * * ?" )
    public void doCompare() {

       // log.info("SpotPerpGapService do compare........");

        HttpHeaders headers = new HttpHeaders();
        HttpEntity entity = new HttpEntity(headers);

        int bnSymbolSize = MarketCache.mxSpotTradeCache.size();
        int mxSymbolSize = MarketCache.mxPerpTradeCache.size();

        for (Map.Entry<String, TradeEvent> mxSpotTrade : MarketCache.mxSpotTradeCache.entrySet()) {
                TradeEvent mxSpotTradeEvent = mxSpotTrade.getValue();
                String symbol = mxSpotTrade.getKey();

                Instant instant = Instant.ofEpochMilli(mxSpotTradeEvent.getTradeTime());
                LocalDateTime spotTradeTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());


                if (MarketCache.mxPerpTradeCache.containsKey(symbol)) {
                    MxTickerEvent mxPerpTradeEvent = MarketCache.mxPerpTradeCache.get(symbol);
                    BigDecimal spotPrice = mxSpotTradeEvent.getPrice();
                    BigDecimal perpPrice = mxPerpTradeEvent.getPrice();

                    Instant instant2 = Instant.ofEpochMilli(mxPerpTradeEvent.getTradeTime());
                    LocalDateTime perpTradeTime = LocalDateTime.ofInstant(instant2, ZoneId.systemDefault());


                    BigDecimal gapRatio = spotPrice.subtract(perpPrice).divide(spotPrice,4, RoundingMode.UP).abs();

                    if(gapRatio.compareTo(BigDecimal.valueOf(BeanConfig.SPOT_PERP_GAP))>0
                           && Math.abs(Duration.between(spotTradeTime, perpTradeTime).toMinutes())<1
                            && BeanConfig.SPOTPERP_GAP_EXCLUDE_LIST.indexOf(symbol) < 0

                    ){

                        BigDecimal bidAskGapRatio = mxPerpTradeEvent.getMaxBidPrice().subtract(mxPerpTradeEvent.getMinAskPrice()).divide(mxPerpTradeEvent.getMaxBidPrice(),4, RoundingMode.UP).abs();
                        if( bidAskGapRatio.compareTo(BigDecimal.valueOf(BeanConfig.BID_ASK_GAP)) < 0){
                            String msg = symbol+ "超过价差:"+ gapRatio.multiply(BigDecimal.valueOf(100)) +"% 盘口价差" + bidAskGapRatio.multiply(BigDecimal.valueOf(100))
                                    + "% mx现货价格:" + spotPrice + " 合约价格:"+ perpPrice + "最佳买单:" + mxPerpTradeEvent.getMaxBidPrice() +" 最佳卖单:"+ mxPerpTradeEvent.getMinAskPrice() +
                             "现货交易时间:"+ spotTradeTime + " 合约交易时间:"+ perpTradeTime;

                            log.info( msg);
                            restTemplate.getForEntity(fanwanUrl+msg,String.class);
                            MarketCache.mxSpotTradeCache.remove(symbol);
                            MarketCache.mxPerpTradeCache.remove(symbol);
                        }else{
                            log.info( "盘口价差过大 "+ symbol +"价差 " + bidAskGapRatio + " 最佳买单" +  mxPerpTradeEvent.getMaxBidPrice() +" 最佳卖单:"+ mxPerpTradeEvent.getMinAskPrice() );
                        }
                    }
                }
        }
    }
}
