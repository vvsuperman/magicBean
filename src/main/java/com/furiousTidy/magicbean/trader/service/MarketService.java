package com.furiousTidy.magicbean.trader.service;

import com.alibaba.excel.EasyExcel;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.binance.client.model.enums.CandlestickInterval;
import com.binance.client.model.enums.PeriodType;
import com.binance.client.model.event.SymbolBookTickerEvent;
import com.binance.client.model.market.*;
import com.binance.client.model.trade.Order;
import com.furiousTidy.magicbean.apiproxy.FutureSyncClientProxy;
import com.furiousTidy.magicbean.apiproxy.SpotSyncClientProxy;
import com.furiousTidy.magicbean.config.BeanConfig;
import com.furiousTidy.magicbean.trader.TradeDto.DVolData;
import com.furiousTidy.magicbean.trader.TradeDto.DVolElement;
import com.furiousTidy.magicbean.trader.TradeDto.MarketOrderBook;
import com.furiousTidy.magicbean.util.BeanConstant;
import com.furiousTidy.magicbean.util.BinanceClient;
import com.furiousTidy.magicbean.util.MarketCache;
import com.furiousTidy.magicbean.util.TradeUtil;
import com.google.gson.JsonObject;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.QueryApi;
import com.influxdb.client.WriteApiBlocking;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import lombok.extern.slf4j.Slf4j;
//import org.influxdb.InfluxDB;
//import org.influxdb.dto.Point;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.*;

@Component
@Slf4j
public class MarketService {


    @Autowired
    BinanceClient binanceClient;

    @Autowired
    InfluxDBClient influxDBClient;

    @Autowired
    TradeUtil tradeUtil;

    @Autowired
    FutureSyncClientProxy futureSyncClientProxy;

    @Autowired
    SpotSyncClientProxy spotSyncClientProxy;

    @Autowired
    RestTemplate restTemplate;


    static String DVOL_URL = "https://app.pinkswantrading.com/graphql";

    static String BN_URL_SPOT=
            "https://api.coinmarketcap.com/data-api/v3/exchange/market-pairs/latest?slug=binance&category=spot&start=1&limit=100";

    static String BN_URL_PERP=
            "https://api.coinmarketcap.com/data-api/v3/exchange/market-pairs/latest?slug=binance&category=perpetual&start=1&limit=100";

    static String OK_URL_SPOT=
            "https://api.coinmarketcap.com/data-api/v3/exchange/market-pairs/latest?slug=okx&category=spot&start=1&limit=100";

    static String OK_URL_PERP=
            "https://api.coinmarketcap.com/data-api/v3/exchange/market-pairs/latest?slug=okx&category=perpetual&start=1&limit=100";


    static String OK_URL="https://coinmarketcap.com/exchanges/okx/";

    static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    static final DateTimeFormatter formatter2 = DateTimeFormatter.ofPattern("yyyy-MM-dd ");


    static final String timeStr = "yyyy-MM-dd";

//    static final List<String> symbolList = Arrays.asList("btcusdt","ethusdt","bnbusdt","xrpusdt","adausdt","solusdt","dogeusdt","dotusdt"
//    ,"shibiusdt","sethusdt","trxusdt","maticusdt","avaxusdt","wbtcusdt","leousdt","uniusdt","atomusdt","etcusdt","okbusdt","ltcusdt","linkusdt",
//            "fttusdt","nearusdt","xlmusdt","crousdt");

    static final List<String> symbolList = Arrays.asList("btcusdt","ethusdt");


    public void get24Volume() throws Exception{

        HttpHeaders headers = new HttpHeaders();
        HttpEntity entity = new HttpEntity(headers);

        String PERPETUAL = "perpetual";
        String SPOT = "spot";
        String BN ="Binance";
        String OK ="OKX";

        int bnSpotBtcUSDT=0, bnSpotBtcBUSD=0,okSpotBtcUSDT=0, okSpotBtcBUSD, bnBTCSpotSum =0;
        int bnPerpBtcUSDT=0, bnPerpBtcBUSD=0,okPerpBtcUSDT=0, okPerpBtcBUSD,bnBTCPerpSum =0;

        List<String> volUrl = Arrays.asList(BN_URL_SPOT,BN_URL_PERP,OK_URL_SPOT, OK_URL_PERP);

        for(int i=0;i<volUrl.size();i++){

            ResponseEntity<JSONObject> returnMsg = restTemplate.exchange(volUrl.get(i), HttpMethod.GET,entity,JSONObject.class);

            if(returnMsg != null) {
                JSONObject jsonData = returnMsg.getBody().getJSONObject("data");
                JSONArray jsonMarketPairs = jsonData.getJSONArray("marketPairs");

                for (int j = 0; j < jsonMarketPairs.size(); j++ ) {
                    JSONObject marketObj = (JSONObject) jsonMarketPairs.get(j);
                    String marketPair = String.valueOf(marketObj.get("marketPair"));
                    String category = String.valueOf(marketObj.get("category"));
                    String name = (String) marketObj.get("exchangeName");

                    if (marketPair.equals("BTC/USDT")) {
                        double volume = (double) marketObj.get("volumeUsd");
                        double price = (double) marketObj.get("price");
                        double volumeBtc = volume / price;


                        if(name.equals(BN) && category.equals(SPOT)){

                            bnSpotBtcUSDT = (int) Math.round(volumeBtc);
                            log.info("bnSpotBtcUSDT:" + bnSpotBtcUSDT + ":" + volume);

                        }else if(name.equals(BN) && category.equals(PERPETUAL)){

                            bnPerpBtcUSDT = (int) Math.round(volumeBtc);
                            log.info("bnPerpBtcUSDT:" + bnPerpBtcUSDT + ":" + volume);

                        }else if(name.equals(OK) && category.equals(SPOT)) {

                            okSpotBtcUSDT = (int) Math.round(volumeBtc);
                            log.info("okSpotBtcUSDT:" + okSpotBtcUSDT + ":" + volume);
                        }
                        else if(name.equals(OK) && marketObj.get("category").equals(PERPETUAL)){

                            okPerpBtcUSDT = (int) Math.round(volumeBtc);
                            log.info("okPerpBtcUSDT:" + okPerpBtcUSDT + ":" + volume);}

                    } else if (marketPair.equals("BTC/BUSD")) {

                        double volume = (double) marketObj.get("volumeUsd");
                        double price = (double) marketObj.get("price");
                        double volumeBtc = volume / price;

                        if(name.equals(BN) && marketObj.get("category").equals(SPOT)){
                            bnSpotBtcBUSD = (int) Math.round(volumeBtc);
                            log.info("bnSpotBtcBUSD:" + bnSpotBtcBUSD + ":" + volume);
                        }else if(name.equals(BN) && marketObj.get("category").equals(PERPETUAL)){
                            bnPerpBtcBUSD = (int) Math.round(volumeBtc);
                            log.info("bnPerpBtcBUSD:" + bnPerpBtcBUSD + ":" + volume);
                        }else if(name.equals(OK) && marketObj.get("category").equals(SPOT)) {
                            okSpotBtcBUSD = (int) Math.round(volumeBtc);
                            log.info("okSpotBtcBUSD:" + okSpotBtcBUSD + ":" + volume);
                        }
                        else if(name.equals(OK) && marketObj.get("category").equals(PERPETUAL)){
                            okPerpBtcBUSD = (int) Math.round(volumeBtc);
                            log.info("okPerpBtcBUSD:" + okPerpBtcBUSD + ":" + volume);}

                    }
                }
        }

        bnBTCSpotSum = bnSpotBtcUSDT + bnSpotBtcBUSD;
        bnBTCPerpSum = bnPerpBtcUSDT + bnPerpBtcBUSD;

        }

                tradeUtil.dingdingSend("24交易情况","币安btc现货总交易量(usdt+busd)为:"+bnBTCSpotSum +
                        ".币安btc合约总交易量(usdt+busd)为:"+bnBTCPerpSum+".Okx btc合约总交易量(usdt)为:"+okSpotBtcUSDT+
                ".Okx btc合约总交易量(usdt)为:"+okPerpBtcUSDT);

//        log.info("币安btc现货总交易量(usdt+busd)为:"+bnBTCSpotSum +
//                ";币安btc合约总交易量(usdt+busd)为:"+bnBTCPerpSum+";Okxbtc现货总交易量(usdt)为:"+okSpotBtcUSDT+
//                ";Okxbtc合约总交易量(usdt)为:"+okPerpBtcUSDT);



    }


    public void getGlobalLongShortAccountRatio() throws Exception{

        MarketCache.futureTickerMap.entrySet().forEach(entry -> {

            String symbol = entry.getKey();
            List<TakerLongShortStat> longShortList
                    = futureSyncClientProxy.getGlobalLongShortAccountRatio( symbol, PeriodType._1d,null,null,1);

            longShortList.forEach(takerLongShortStat -> {
                if(takerLongShortStat.getBuySellRatio().compareTo(new BigDecimal(2)) > 0
//                       || takerLongShortStat.getBuySellRatio().compareTo(new BigDecimal(1)) < 0
                        ){
                    tradeUtil.dingdingSend("多空比超限","symbol=" + symbol + ",多空比" + takerLongShortStat.getBuySellRatio());
                }
            });
        });
    }





    public void calculateDepth () throws Exception{

        WriteApiBlocking writeApi = influxDBClient.getWriteApiBlocking();

        for (String s : symbolList) {
            String symbol = s.toUpperCase();
//            String symbol="BTCUSDT";


            if (MarketCache.futureTickerMap.containsKey(symbol)) {

                SymbolBookTickerEvent sbe = MarketCache.futureTickerMap.get(symbol);
                BigDecimal price = (sbe.getBestBidPrice().add(sbe.getBestAskPrice())).divide(new BigDecimal(2), 4, RoundingMode.HALF_UP);
                BigDecimal maxPrice = price.multiply(BigDecimal.valueOf(1.05));
                BigDecimal minPrice = price.multiply(BigDecimal.valueOf(0.95));

                final BigDecimal[] up5 = new BigDecimal[1];
                up5[0] = BigDecimal.ZERO;
                final BigDecimal[] down5 = new BigDecimal[1];
                down5[0] = BigDecimal.ZERO;

                if( MarketCache.orderBookCache.containsKey(symbol)){
                    MarketOrderBook marketOrderBook = MarketCache.orderBookCache.get(symbol);

                    if(marketOrderBook.getAskMap() != null){
                        marketOrderBook.getAskMap().entrySet().forEach(askEntry -> {

                            if (askEntry.getKey().compareTo(minPrice) > 0 && askEntry.getValue() != null) {
                                down5[0] = down5[0].add(askEntry.getValue());
                            }
                        });
                    }


                    if(marketOrderBook.getBidMap() != null){
                        marketOrderBook.getBidMap().entrySet().forEach(bidEntry -> {
                            if (bidEntry.getKey().compareTo(maxPrice) < 0 && bidEntry.getValue() != null) {
                                up5[0] = up5[0].add(bidEntry.getValue());
                            }
                        });
                    }


                    BigDecimal depth5 = down5[0].add(up5[0]);
                    marketOrderBook.setDepth5(depth5.intValue());

                    Point point = Point.measurement("depthData").addTag("symbol", symbol)
                            .addField("up5", up5[0])
                            .addField("down5", down5[0])
                            .addField("depth5", depth5);
                    writeApi.writePoint(point);
                }




            }
        }
    }


    public void getAndStoreOrderBook () throws Exception{

//            MarketCache.futureTickerMap.entrySet().forEach(entry -> {
                symbolList.forEach(symbol->{

//                String symbol = entry.getKey().toLowerCase();

     //           String symbol = "btcusdt";

                OrderBook orderBook = binanceClient.getFutureSyncClient().getOrderBook(symbol,1000);

                MarketOrderBook marketOrderBook = new MarketOrderBook();
                marketOrderBook.setSymbol(symbol.toUpperCase());

                orderBook.getAsks().forEach(orderBookEntry -> {
                    marketOrderBook.getAskMap().put(orderBookEntry.getPrice(), orderBookEntry.getQty());
                });

                orderBook.getBids().forEach(orderBookEntry -> {
                    marketOrderBook.getBidMap().put(orderBookEntry.getPrice(), orderBookEntry.getQty());
                });

                binanceClient.getFutureSubsptClient().subscribeDiffDepthEvent(symbol, orderBookEvent->{

                    String futrueSymbol = orderBookEvent.getSymbol();

                    if(!MarketCache.orderBookCache.containsKey(futrueSymbol)){
                        MarketCache.orderBookCache.put(futrueSymbol, new MarketOrderBook());

                    }

                    orderBookEvent.getAsks().forEach(orderBookEntry1 -> {

                        Map<BigDecimal,BigDecimal> askMap = MarketCache.orderBookCache.get(futrueSymbol).getAskMap();

                        askMap.put(orderBookEntry1.getPrice(), orderBookEntry1.getQty());

                        if((orderBookEntry1.getQty().compareTo( BigDecimal.ZERO ) == 0)
                                && askMap.containsKey(orderBookEntry1.getPrice())){

                            askMap.remove(orderBookEntry1.getPrice());

                        }


                    });

                    orderBookEvent.getBids().forEach(orderBookEntry2 -> {

                        Map<BigDecimal,BigDecimal> bidMap = MarketCache.orderBookCache.get(futrueSymbol).getBidMap();

                        bidMap.put(orderBookEntry2.getPrice(), orderBookEntry2.getQty());

                        if((orderBookEntry2.getQty().compareTo( BigDecimal.ZERO ) == 0)
                                && bidMap.containsKey(orderBookEntry2.getPrice())) {

                            bidMap.remove(orderBookEntry2.getPrice());

                        }
                    });

                },null);

                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
    }


    public void getDvol() throws Exception{

        WriteApiBlocking writeApi = influxDBClient.getWriteApiBlocking();

        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/json");

        LocalDateTime localDateTime = LocalDateTime.now();
        String dateTimeStart = localDateTime.minusDays(1).format(formatter);
        String dateTimeEnd = localDateTime.plusDays(1).format(formatter);

        List<String> symbolList =  Arrays.asList("BTC","ETH");

        for(String symbol : symbolList){

            String bodyStr = " {\"operationName\":\"dVol\",\"variables\":{\"symbol\":\""+symbol+ "\",\"exchange\":\"deribit\"," +
                    "\"dateStart\":\""+dateTimeStart + "\",\"dateEnd\":\""+ dateTimeEnd +"\",\"interval\":\"1 hr\"},\"query\":" +
                    "\"query dVol($exchange: ExchangeEnumType, $symbol: SymbolEnumType, $interval: String, " +
                    "$dateStart: String, $dateEnd: String) {\\n  dVol: genericDvol(\\n    symbol: $symbol\\n    " +
                    "exchange: $exchange\\n    interval: $interval\\n    dateStart: $dateStart\\n    " +
                    "dateEnd: $dateEnd\\n  ) {\\n    date: timerange\\n    instrument\\n    " +
                    "open\\n    high\\n    low\\n    close\\n    __typename\\n  }\\n}\\n\"}";


            JSONObject jsonObj = JSONObject.parseObject(bodyStr);
            HttpEntity entity = new HttpEntity(jsonObj,headers);
            ResponseEntity<JSONObject> returnMsg = restTemplate.exchange(DVOL_URL, HttpMethod.POST,entity,JSONObject.class);
            Map dVolData = (Map) returnMsg.getBody().get("data");
            List<Map> dVol = (List<Map>) dVolData.get("dVol");

            for( Map dVolElement :dVol){
                Point point = Point.measurement("dVolData").addTag("symbol", symbol)

                        .addField("open",  Double.valueOf(dVolElement.get("open").toString()))
                        .addField("close", Double.valueOf(dVolElement.get("close").toString()))
                        .addField("high", Double.valueOf(dVolElement.get("high").toString()))
                        .addField("low", Double.valueOf(dVolElement.get("low").toString()))
                        .time(Long.parseLong((String) dVolElement.get(("date"))), WritePrecision.MS)
                        ;
                writeApi.writePoint(point);
            }
        }

    }


    public void getKlineTest(){

        List<Candlestick> candList= new ArrayList<>();

        try {
             candList = futureSyncClientProxy.getCandlestick("BTCUSDT");
        }catch (Exception ex){
            log.error("get candlist exception" + ex);
        }
        log.info("candList:" + candList);
    }


    public void getAndStoreKLineHis(){

        LocalDateTime localDateTimeStart = LocalDateTime.parse("2020-01-01T00:00:01");
        LocalDateTime localDateTimeEnd = localDateTimeStart.plusDays(30);

        long startTime = localDateTimeStart.toInstant(ZoneOffset.UTC).toEpochMilli();
        long endTime = localDateTimeEnd.toInstant(ZoneOffset.UTC).toEpochMilli();

        List<Candlestick> candList = new ArrayList<>();

        List<List<Object>> dataList = new ArrayList<>();


        while ( localDateTimeEnd.isBefore( LocalDateTime.now())){

            candList = futureSyncClientProxy.getCandlestickHis("ETHUSDT",
                    CandlestickInterval.DAILY,startTime,endTime,720);

            for(int i=0;i<candList.size();i++) {

                Candlestick candlestick = candList.get(i);
                List<Object> data = new ArrayList<>();
                data.add( LocalDateTime.ofInstant( Instant.ofEpochMilli(candlestick.getCloseTime()+1), ZoneId.systemDefault()).format(formatter));
                data.add( candlestick.getClose());
                dataList.add(data);

            }

            localDateTimeStart = localDateTimeEnd;
            localDateTimeEnd = localDateTimeStart.plusDays(30);
            startTime = localDateTimeStart.toInstant(ZoneOffset.UTC).toEpochMilli();
            endTime = localDateTimeEnd.toInstant(ZoneOffset.UTC).toEpochMilli();

        }

        List<List<String>> headList = new ArrayList<>();
        List<String> head0 = new ArrayList<>();
        head0.add("date");
        List<String> head1 = new ArrayList<>();
        head1.add("eth_price");
        headList.add(head0);
        headList.add(head1);

        String fileName = "/Users/fw/Documents/arbstrategy/perp/project/perpSimulator.xlsx";
        EasyExcel.write(fileName).head(headList).sheet("模板").doWrite(dataList);


    }


    //k线获取
    public void getAndStoreAllKLine() throws InterruptedException {

        log.info("getAndStoreAllKLine begin......");

        WriteApiBlocking writeApi = influxDBClient.getWriteApiBlocking();

        List<Point> pointList = new ArrayList<>();

        for(Map.Entry<String, SymbolBookTickerEvent> entrySet: MarketCache.futureTickerMap.entrySet()){

            String symbol = entrySet.getKey();

            List<Candlestick> candList = new ArrayList<>();

            try {
                candList = futureSyncClientProxy.getCandlestick(symbol);
            }catch (Exception ex){
                log.error("get candlist exception" + ex);
            }

            if(candList.isEmpty()){
                continue;
            }

            for(int i=0;i<candList.size();i++){

                Candlestick candlestick = candList.get(i);

                Point point = Point.measurement("candleData").addTag("symbol", symbol)
                        .addField("openTime", candlestick.getOpenTime())
                        .addField("closeTime", candlestick.getCloseTime())
                        .addField("open", candlestick.getOpen())
                        .addField("close", candlestick.getClose())
                        .addField("high", candlestick.getHigh())
                        .addField("low", candlestick.getLow())
                        .addField("numTrade", candlestick.getNumTrades())
                        .time(candlestick.getCloseTime()+1,WritePrecision.MS)
                        ;

//                pointList.add(point);
                writeApi.writePoint(point);
            }


            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }

        log.info("get k line finished......." + MarketCache.futureTickerMap.size());
//        writeApi.writePoints(pointList);



    }

    @Async
    public void getOpenInterest( ) throws InterruptedException {

        WriteApiBlocking writeApi = influxDBClient.getWriteApiBlocking();


       while(true){

           int i=0;

           for(Map.Entry<String, SymbolBookTickerEvent> entrySet: MarketCache.futureTickerMap.entrySet()){


               String symbol = entrySet.getKey();

               List<Point> pointList = new ArrayList<Point>();

               List<OpenInterestStat> listOpenInterest =  new ArrayList<>();

               int j = 0;

               while(listOpenInterest.isEmpty() && j++ < 6 ){

                   try {

                       listOpenInterest =  binanceClient
                               .getFutureSyncClient().getOpenInterestStat( symbol, PeriodType._1h, null, null, 1 );

                   }catch (Exception ex){

                       log.error("get interest error" + ex );
                   }
               }


               for(OpenInterestStat openInterestStat: listOpenInterest){

                   Point point = Point.measurement("symbolData").addTag("symbol", symbol).addField("openInterest", openInterestStat.getSumOpenInterest())

                           .addField("openInterestValue", openInterestStat.getSumOpenInterestValue())

                           .time(openInterestStat.getTimestamp(), WritePrecision.MS);

                   writeApi.writePoint(point);

               }

               Thread.sleep(50);

               i++;

           }
           log.info("add symbol finished, sleep now, count={}",i);



           Thread.sleep(60000);

       }

    }

    public void alertOpenInterest() throws Exception{

        JSONArray jsonArray = JSONArray.parseArray(BeanConfig.OPEN_INTEREST_ARRAY);

        int sum = 0;
        int maxRatio = 0;
        String maxSymbol = "";
        int maxGap = 0;

        for ( int i =0 ;i< jsonArray.size();i++){
            JSONObject jsonObject = (JSONObject) jsonArray.getJSONObject(i);
            Iterator it = jsonObject.keySet().iterator();

            int gap = 0;
            int threshold = 0;

            while(it.hasNext()){
                String key = it.next().toString();

                if(key.equals("gap")){
                    gap = Integer.parseInt((String) jsonObject.get(key));
                }else if(key.equals("threshold")){
                    threshold = Integer.parseInt((String) jsonObject.get(key));
                }

            }

            LocalDateTime endTime = OffsetDateTime.now(ZoneOffset.UTC).toLocalDateTime().plusNanos(11000000);

            LocalDateTime middleTime = endTime.minusHours(gap);

            LocalDateTime startTime = middleTime.minusHours(gap);

            log.info("time log startTime ={}, midleTime={}, endTime={}", startTime, middleTime, endTime);

            for(Map.Entry<String, SymbolBookTickerEvent> entrySet: MarketCache.futureTickerMap.entrySet()){

                String symbol = entrySet.getKey();

                String flux1 = "from(bucket: \"dengTrade\")\n" +
                        "  |> range(start: "+ middleTime + "Z, stop:" + endTime + "Z)\n" +
                        "  |> filter(fn: (r) => r[\"_measurement\"] == \"symbolData\")\n" +
                        "  |> filter(fn: (r) => r[\"symbol\"] == \"" + symbol +"\")\n" +
                        "  |> filter(fn: (r) => r[\"_field\"] == \"openInterestValue\")\n" +
                        "  |> yield(name: \"mean\")";


                String flux2 = "from(bucket: \"dengTrade\")\n" +
                        "  |> range(start: "+ startTime + "Z, stop:" + middleTime + "Z)\n" +
                        "  |> filter(fn: (r) => r[\"_measurement\"] == \"symbolData\")\n" +
                        "  |> filter(fn: (r) => r[\"symbol\"] == \"" + symbol +"\")\n" +
                        "  |> filter(fn: (r) => r[\"_field\"] == \"openInterestValue\")\n" +
                        "  |> yield(name: \"mean\")";


                QueryApi queryApi = influxDBClient.getQueryApi();

                List<FluxTable> tables1 = new ArrayList<>();
                List<FluxTable> tables2 = new ArrayList<>();

                try{
                    tables1 = queryApi.query(flux1);
                    tables2 = queryApi.query(flux2);
                }catch (Exception ex){
                    log.error("influx get error:" + ex );
                }

                long interestNow = 0;
                long interestBefore = 0;

                for (FluxTable fluxTable : tables1) {
                    List<FluxRecord> records = fluxTable.getRecords();
                    for (FluxRecord fluxRecord : records) {
                        interestNow = new BigDecimal(fluxRecord.getValueByKey("_value").toString()).longValue();
                    }
                }

                for (FluxTable fluxTable : tables2) {
                    List<FluxRecord> records = fluxTable.getRecords();
                    for (FluxRecord fluxRecord : records) {
                        interestBefore = new BigDecimal(fluxRecord.getValueByKey("_value").toString()).longValue();
                    }
                }



                if(interestBefore != 0){

                    int ratio = (int) ((interestNow - interestBefore) * 100 / interestBefore);

                    if(ratio > maxRatio){
                        maxRatio = ratio;
                        maxSymbol = symbol;
                        maxGap = gap;
                    }

                    if( ratio >= threshold){

                        sum++;

                       tradeUtil.dingdingSend(symbol+"持仓异动", symbol + "持仓比例在" + gap + "小时内上升" + ratio + "%");

                    }
                }
            }
        }

        if (sum == 0 ){
            tradeUtil.dingdingSend("未见持仓异动", "最大持仓变动为" + maxSymbol + "在"+ maxGap+ "上升" + maxRatio + "%");
        }
    }





    public void getOpenInterestHis( Long startTime, Long endTime, Integer limit) {


    }

    public static  void main(String[] args){

        LocalDateTime localDateTime = LocalDateTime.now();
        String dateTime = localDateTime.format(formatter);

        System.out.println(dateTime);


//       long interestNow=2388376094l, interestBefore=2349115147l;
//        int ratio = (int) ((interestNow - interestBefore) * 100 / interestBefore);
//
//        System.out.println(ratio);



//        JSONArray jsonArray = JSONArray.parseArray(BeanConfig.OPEN_INTEREST_ARRAY);
//
//        for ( int i =0 ;i< jsonArray.size();i++){
//            JSONObject jsonObject = (JSONObject) jsonArray.getJSONObject(i);
//            Iterator it = jsonObject.keySet().iterator();
//            while(it.hasNext()){
//                String key = it.next().toString();
//                log.info("key:"+ key);
//                log.info("value" + jsonObject.get(key));
//            }
//
//
//        }
    }



}
