package com.furiousTidy.magicbean.subscription;

import com.binance.client.model.market.ExchangeInformation;
import com.furiousTidy.magicbean.apiproxy.FutureSyncClientProxy;
import com.furiousTidy.magicbean.apiproxy.SpotSyncClientProxy;
import com.furiousTidy.magicbean.influxdb.InfluxDbConnection;
import com.furiousTidy.magicbean.util.BeanConstant;
import com.furiousTidy.magicbean.util.BinanceClient;
import com.furiousTidy.magicbean.util.MarketCache;
import lombok.extern.slf4j.Slf4j;
import org.influxdb.InfluxDB;
import org.influxdb.dto.BatchPoints;
import org.influxdb.dto.Point;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class PreTradeService {

    @Autowired
    SpotSyncClientProxy spotSyncClientProxy;

    @Autowired
    FutureSyncClientProxy futureSyncClientProxy;

    @Autowired
    InfluxDbConnection influxDbConnection;


    //get all ticks in binance and store in the influxdb
    public void storeTicks() throws InterruptedException {
        TreeMap<BigDecimal,String > ratioMap = new TreeMap<>(
                (o1, o2) -> o2.compareTo(o1));
        BigDecimal futureBidPrice,futureAskPrice,spotBidPrice,spotAskPrice,openRatio,closeRatio,fundingRate,premiumRatio;
        String symbol;
        Point point;
        do{
            BatchPoints batchPoints = BatchPoints.database("magic_bean").retentionPolicy("autogen").
                    consistency(InfluxDB.ConsistencyLevel.ALL).build();
            ratioMap.clear();
            for(Map.Entry<String, HashMap<String, BigDecimal>> entrySet:MarketCache.futureTickerMap.entrySet()){
                symbol = entrySet.getKey();
                futureBidPrice = entrySet.getValue().get(BeanConstant.BEST_BID_PRICE);
                futureAskPrice = entrySet.getValue().get(BeanConstant.BEST_ASK_PRICE);
                if(!MarketCache.spotTickerMap.containsKey(symbol))  continue;
                spotAskPrice = MarketCache.spotTickerMap.get(symbol).get(BeanConstant.BEST_ASK_PRICE);
                spotBidPrice = MarketCache.spotTickerMap.get(symbol).get(BeanConstant.BEST_BID_PRICE);

                fundingRate = MarketCache.markPriceEventMap.containsKey(symbol)?
                        MarketCache.markPriceEventMap.get(symbol).getFundingRate():BigDecimal.ZERO;
                point = Point.measurement(BeanConstant.SYMBOL_TICKS_INFO)
                        .time(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                        .tag(BeanConstant.EXCHANGE, BeanConstant.BINANCE)
                        .tag(BeanConstant.SYMBOL,symbol)
                        .addField(BeanConstant.FUTURE_BID_RPICE,futureBidPrice)
                        .addField(BeanConstant.FUTURE_ASK_PRICE,futureAskPrice)
                        .addField(BeanConstant.SPOT_BID_PRICE,spotBidPrice)
                        .addField(BeanConstant.SPOT_ASK_PRICE, spotAskPrice)
                        .addField(BeanConstant.FUNDING_RATE,fundingRate)
                        .build();

                batchPoints.point(point);
            }
            influxDbConnection.batchInsert(batchPoints);
            log.info("ratio:{}",ratioMap);
            Thread.sleep(1000);
        }while (true);
    }

    //get exchange info for future
    public void futureExchangeInfo(){
       ExchangeInformation exchangeInfo = null;
       try {
           exchangeInfo=  futureSyncClientProxy.getExchangeInfo();
       }catch (Exception ex){
            log.error("get future Exchange exception:{}",ex);
       }

       exchangeInfo.getSymbols().forEach(exchangeInfoEntry -> {
           MarketCache.futureInfoCache.put(exchangeInfoEntry.getSymbol(),exchangeInfoEntry);
       });
    }

    //get exchange info for spot
    public void spotExchangeInfo(){
        try{
            spotSyncClientProxy.getExchangeInfo().getSymbols().forEach(symbolInfo -> {
                MarketCache.spotInfoCache.put(symbolInfo.getSymbol(),symbolInfo);
            });
        }catch(Exception ex){
            log.error("get spot exchange info error:{}",ex);
        }

    }
}
