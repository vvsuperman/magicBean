package com.furiousTidy.magicbean.subscription;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.binance.api.client.domain.event.TickerEvent;
import com.binance.api.client.domain.event.TradeEvent;
import com.binance.client.model.event.SymbolTickerEvent;
import com.furiousTidy.magicbean.trader.TradeDto.MxTickerEvent;
import com.furiousTidy.magicbean.util.BeanConstant;
import com.furiousTidy.magicbean.util.MarketCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class MxSub {

    @Autowired
    RestTemplate restTemplate;


    String perpInfoUrl = "https://contract.mexc.com/api/v1/contract/detail";

    String spotInfoUrl =  "https://api.mexc.com/api/v3/exchangeInfo";


    String perpUrl= "https://contract.mexc.com/api/v1/contract/ticker";



    public void getSpotPerpInfo(){
        log.info("get mx spot and perp exchange info");

        ResponseEntity<JSONObject> spotJsonObject = restTemplate.getForEntity(spotInfoUrl,JSONObject.class);
        List<Map> spotSymbolList = (List<Map>) spotJsonObject.getBody().get("symbols");
        spotSymbolList.forEach(symbolMap ->{
            String symbol = (String) symbolMap.get("symbol");
            MarketCache.MX_SPOT_SYMBOL.add(symbol);

        });

        ResponseEntity<JSONObject> perpJsonObject = restTemplate.getForEntity(perpInfoUrl,JSONObject.class);
        List<Map> perpSymbolList = (List<Map>) perpJsonObject.getBody().get("data");
        perpSymbolList.forEach(symbolMap->{
            String symbol = (String) symbolMap.get("symbol");
            MarketCache.MX_PERP_SYMBOL.add(symbol.replace("_",""));
        });

        MarketCache.MX_SPOT_SYMBOL.forEach(spotSymbol->{
            MarketCache.MX_PERP_SYMBOL.forEach(perpSymbol->{
                if(spotSymbol.equals(perpSymbol)){
                    MarketCache.MX_SPOT_PERP_SYMBOL.add(spotSymbol);
                }
            });
        });
    }


    public void subAllSpotTrade() throws Exception {

        log.info("get mx spot trade");

        String symbolUrl = "https://api.mexc.com/api/v3/defaultSymbols";

        Object object = new Object();
        ResponseEntity<JSONObject> jsonObject = restTemplate.getForEntity(symbolUrl,JSONObject.class);
       // JSONObject symbols = jsonObject.getBody();
        List<String> symbolList = (List<String>) jsonObject.getBody().get("data");

        int j=0;
        int symbolListSize = symbolList.size();
        for(;j<symbolListSize;j++){
            String subKey = "{ \"method\":\"SUBSCRIPTION\", \"params\":[";
            int i=j;
            for(; i < Math.min(j+25,symbolListSize);i++){
                String symbolKey =  "\"spot@public.deals.v3.api@"+symbolList.get(i)+"\",";
                subKey += symbolKey;
            }
            j=i-1;
            String adjustSubKey = subKey.substring(0, subKey.length()-1);
            adjustSubKey+="]}";

            ReConnectWebSocketClient client =
                    new ReConnectWebSocketClient(
                            new URI("wss://wbs.mexc.com/ws"),
                            "spot@public.deals.v3.api@",
                            // 字符串消息处理
                            msg -> {
                                // todo 字符串消息处理
                                //System.out.println("字符串消息:" + msg);
                                JSONObject tradeObject = JSON.parseObject(msg);
                                TradeEvent tradeEvent = new TradeEvent();
                                if(tradeObject.containsKey("c")){
                                    tradeEvent.setSymbol((String) tradeObject.get("s"));
                                    JSONObject tradeOBj = ((List<JSONObject>)((JSONObject)tradeObject.get("d")).get("deals")).get(0);
                                    tradeEvent.setPrice(BigDecimal.valueOf(Double.valueOf((String) tradeOBj.get("p"))));
                                    tradeEvent.setTradeTime( (Long)tradeOBj.get("t"));
                                    tradeEvent.setQty(BigDecimal.valueOf(Double.valueOf((String) tradeOBj.get("v"))));
                                    MarketCache.mxSpotTradeCache.put(tradeEvent.getSymbol(), tradeEvent);
                                }
                            },
                            null,
                            // 异常回调
                            error -> {
                                // todo 字符串消息处理
                                System.out.println("异常:" + error.getMessage());
                            });
            client.connect();
            //String tradeStr = "{ \"method\":\"SUBSCRIPTION\", \"params\":[\"spot@public.deals.v3.api@BTCUSDT\",\"spot@public.deals.v3.api@ETHUSDT\"]}";
            client.send(adjustSubKey);
        }
    }

    @Scheduled(cron = "0/5 * * * * ?" )
    public void subAllPerpTrade() throws Exception {


        ResponseEntity<JSONObject> spotJsonObject = restTemplate.getForEntity(perpUrl,JSONObject.class);
        List<Map> perpTickerList = (List<Map>) spotJsonObject.getBody().get("data");


        perpTickerList.forEach(tickerMap ->{

            MxTickerEvent tradeEvent = new MxTickerEvent();
            if (tickerMap.containsKey("lastPrice")) {
                tradeEvent.setSymbol(((String) tickerMap.get("symbol")).replaceAll("_",""));

                tradeEvent.setPrice( getRealPrice( tickerMap.get("lastPrice")));
                tradeEvent.setMaxBidPrice(getRealPrice(tickerMap.get("bid1")));
                tradeEvent.setMinAskPrice(getRealPrice(tickerMap.get("ask1")));

                tradeEvent.setTradeTime( (Long)tickerMap.get("timestamp"));
                MarketCache.mxPerpTradeCache.put(tradeEvent.getSymbol(), tradeEvent);
            }
        });





    }

    private  BigDecimal getRealPrice( Object price) {

        if (price.getClass().toString().equals("class java.math.BigDecimal")) {
            return (BigDecimal) price;
        } else if (price.getClass().toString().equals("class java.lang.Integer")) {
            return new BigDecimal((Integer) price);
        }  else if (price.getClass().toString().equals("class java.lang.Double")) {
             return new BigDecimal((Double) price);
    }
        log.info("can not find class" + price.getClass().toString());
        return BigDecimal.ZERO;
    }


}
