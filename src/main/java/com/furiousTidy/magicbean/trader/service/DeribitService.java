package com.furiousTidy.magicbean.trader.service;


import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.furiousTidy.magicbean.apiproxy.DeribitClientProxy;
import com.furiousTidy.magicbean.dbutil.dao.OptionDao;
import com.furiousTidy.magicbean.dbutil.model.OptionModel;
import com.furiousTidy.magicbean.trader.TradeDto.OptionData;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.WriteApiBlocking;
import com.influxdb.client.write.Point;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.tomcat.jni.Local;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Pattern;

//get deribit option chain info
@Component
@Slf4j
public class DeribitService {


    @Autowired
    InfluxDBClient influxDBClient;

    //https://history.deribit.com/api/v2/public/get_last_trades_by_currency_and_time?currency=ETH&start_timestamp=1630490400000&count=1000&include_old=true
    static String url = "https://history.deribit.com/api/v2/public/get_last_trades_by_currency_and_time?currency=ETH" +
            "&count=1000&include_old=true&start_timestamp=";

    @Autowired
    DeribitClientProxy deribitClientProxy;

    @Autowired
    OptionDao optionDao;

    @Autowired
    private SqlSessionFactory sqlSessionFactory;

    static DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    String regex = ".*-(C|P)\\b";


    public void getOptionInfo() throws InterruptedException {
        String strDateTime = "2022-08-01T00:00:01";
        String strDateTimeEnd = "2023-02-12T00:00:01";

//      1 按照天循环
//      2 获取每天第一次取的，所有的instruments,筛选出期权,算出初次价格的market_price，
//      3 存入mysql
        int threadNum=10;
        LocalDateTime startTime = LocalDateTime.parse(strDateTime);
        LocalDateTime dateTimeEnd = LocalDateTime.parse(strDateTimeEnd);

        while(startTime.isBefore(dateTimeEnd)){
            //先按照天数进行分组
            LocalDateTime monthEnd = startTime.plusMonths(1);
            long duration = startTime.toLocalDate().until(monthEnd.toLocalDate(), ChronoUnit.DAYS);

            CountDownLatch countDownLatch = new CountDownLatch((int) duration);
            //ExecutorService es = Executors.newFixedThreadPool((int) duration);
            ExecutorService es = Executors.newFixedThreadPool(threadNum);

            //获取一个月内option数据，完成后再继续下一个月
            for(int i =0; i<duration ; i++){
                LocalDateTime endTime3Days = startTime.plusDays(1);
                final LocalDateTime endTime = endTime3Days.isBefore(dateTimeEnd)?endTime3Days:dateTimeEnd;

                try {
                    es.submit(new OptionFetch(startTime,endTime,countDownLatch));
                }catch (Exception ex){
                    log.error("option fetch thread error....{}",ex);
                }
                startTime = startTime.plusDays(1);
            }
            //阻塞本月线程
            try{
                countDownLatch.await();
            }catch (Exception ex){
                log.error("option fetch thread error....{}",ex);
            }
            es.shutdown();
            log.info("{}月执行完毕",startTime.getMonth().getValue()-1);

        }
    }

    class OptionFetch implements Callable {

        LocalDateTime dateTimeStart;
        LocalDateTime dateTimeEnd;

        LocalDateTime startTime;

        CountDownLatch countDownLatch;


        public OptionFetch(LocalDateTime dateTimeStart, LocalDateTime dateTimeEnd, CountDownLatch countDownLatch) {
            this.startTime = dateTimeStart;
            this.dateTimeStart = dateTimeStart;
            this.dateTimeEnd = dateTimeEnd;
            this.countDownLatch = countDownLatch;
        }

        @Override
        public Object call() throws Exception {
            //Thread.sleep(1000);
           // this.countDownLatch.countDown();

            log.info("option线程数据获取开始， dateStart={},dateEnd={}", startTime,dateTimeEnd);
            //return "success";
            return doOption();
        }

        @NotNull
        private Integer doOption() throws InterruptedException {

            Integer count = 0;
            int round = 0;

            while (dateTimeStart.isBefore(dateTimeEnd)) {

                round++;

                long startTime = dateTimeStart.toInstant(ZoneOffset.of("+08:00")).toEpochMilli();

                String dUrl = url + startTime;

                ResponseEntity<JSONObject> returnMsg = null;

                try {
                    log.info("get data from deribit begin,startTime={},round={}",dateTimeStart.toLocalDate(),round);
                    long queryStart = System.currentTimeMillis();
                    returnMsg = deribitClientProxy.getOptionInfo(dUrl);
                    //log.info("get data from deribit end,startTime={},round={},duration={}",dateTimeStart.toLocalDate(),round,System.currentTimeMillis() - queryStart);
                } catch (Exception ex) {
                    //log.error("get option info exception: startTime={}, round={}, ex={}" ,dateTimeStart.toLocalDate(),round, ex);
                }

                if (returnMsg != null) {
                    JSONObject result = returnMsg.getBody().getJSONObject("result");
                    JSONArray trades = result.getJSONArray("trades");
                    long start = System.currentTimeMillis();
                    long saveStart = System.currentTimeMillis();
                    try {
                        log.info("save data into db begin,startTime={},round={}",dateTimeStart, round);

                        SqlSession sqlSession = sqlSessionFactory.openSession(ExecutorType.BATCH, false);

                        for (Object trade : trades) {
                            JSONObject jsonTrade = (JSONObject)trade;

                            String instrumentName = jsonTrade.getString("instrument_name");
                            Timestamp t = new Timestamp(jsonTrade.getLong("timestamp"));
                            LocalDateTime instrumentDateTime = t.toLocalDateTime();

                            if (Pattern.matches(regex, instrumentName) && instrumentDateTime.isBefore(dateTimeEnd)) {
                                saveOption(jsonTrade);
                                count++;
                             //   log.info("count={}, trade={}", count, jsonTrade);
                            }
                        }

                        sqlSession.commit();
                        sqlSession.clearCache();

                    } catch (Exception ex) {
                        log.error("save to db error, exception={}", ex);
                    }
                    //将datetime start移到队列中最后一个数据
                    JSONObject trade1 = (JSONObject) trades.get(trades.size() - 1);
                    long timeStamp = (long) trade1.get("timestamp");

                    LocalDateTime lastRecordTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(timeStamp), ZoneId.systemDefault());
                    log.info("save data into db end,startTime={},round={},lastRecordTime ={};duration={}",dateTimeStart.toLocalDate(),round,lastRecordTime, System.currentTimeMillis()-saveStart);
                    dateTimeStart = lastRecordTime;
                    //log.info("插入option数据完成，datetime={} 耗时 {}", dateTimeStart, System.currentTimeMillis() - start);

                }//if
                //防止频繁请求被封
                //Thread.sleep(1000);
            }//while

            Date dateStart = new Date(startTime.toInstant(ZoneOffset.of("+08:00")).toEpochMilli());
            Date dateEnd = new Date(dateTimeEnd.toInstant(ZoneOffset.of("+08:00")).toEpochMilli());
//            Date dateStart = new Date(startTime.toInstant(ZoneOffset.UTC).toEpochMilli());
//            Date dateEnd = new Date(dateTimeEnd.toInstant(ZoneOffset.UTC).toEpochMilli());
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            int optionCount = optionDao.getOptionCount(dateStart, dateEnd);

            if(optionCount == count){
                log.info("option线程数据获取完毕，结果相等， dateStart={},dateEnd={}, databaseCount={}, actualCount={}", startTime,dateTimeEnd, optionCount, count);
            }else{
                log.error("option线程数据获取完毕,结果不等， startTime={}, endTime={}, databaseCount={}, actualCount={}"
                        ,startTime,dateTimeEnd, optionCount,count);
            }
            this.countDownLatch.countDown();
            return count;
        }

        private void saveOption(JSONObject trade) {

                int num = optionDao.getOptionById(trade.getString("trade_id"));
                if(num > 0) return;


                OptionModel optionModel = new OptionModel();
                String instrumentName = trade.getString("instrument_name");

                optionModel.setInstrumentName(instrumentName);
                optionModel.setSymbol(instrumentName.substring(0, instrumentName.indexOf("-")));
                optionModel.setTradeId(trade.getString("trade_id"));
                optionModel.setPrice(trade.getFloat("price"));
                optionModel.setMarkPrice(trade.getFloat("mark_price"));
                optionModel.setIv(trade.getFloat("iv"));
                optionModel.setIndexPrice(trade.getFloat("index_price"));
                optionModel.setAmount(trade.getFloat("amount"));
                optionModel.setDirection(trade.getString("direction"));
                optionModel.setTickDirection(trade.getInteger("tick_direction"));
                optionModel.setTradeTime(new Date(new Timestamp(trade.getLong("timestamp")).getTime()));

                //optionModel.setTradeTime(LocalDateTime.ofEpochSecond(trade.getInteger("timestamp") / 1000, 0, ZoneOffset.ofHours(8)));
                optionDao.saveOption(optionModel);
            }
        }


         //         @NotNull
//         private String savaOptionBak() {
//             Set intrumentSet = new HashSet<String>();
//
//             String fileName = "/Users/fang/Documents/arb_strategy/perp/py_project/data/2021/deribt"+i+".xlsx";
//
//             ExcelWriter excelWriter = EasyExcel.write(fileName, OptionData.class).build();
//             WriteSheet writeSheet = EasyExcel.writerSheet("deribit").build();
//             List<OptionData> optionDataList = new ArrayList<>();
//
//             while (dateTimeStart.isBefore(dateTimeEnd)) {
//
//                 long startTime = dateTimeStart.toInstant(ZoneOffset.UTC).toEpochMilli();
//                 String dUrl = url + startTime;
//
//                 log.info(" get option info, time={}", dateTimeStart);
//                 ResponseEntity<JSONObject> returnMsg = null;
//
//                 try {
//                     returnMsg = deribitClientProxy.getOptionInfo(dUrl);
//                 }catch (Exception ex){
//                     log.error("get option info exception:"+ex);
//                     excelWriter.finish();
//                 }
//
//                 if(returnMsg!=null){
//
//                     JSONObject result = returnMsg.getBody().getJSONObject("result");
//                     JSONArray trades = result.getJSONArray("trades");
//                     List<Point> points = new ArrayList<>();
//
//                     for (int i = 0; i < trades.size(); i++) {
//                         JSONObject trade = (JSONObject) trades.get(i);
//                         String instrumentName = trade.getString("instrument_name");
//                         if (Pattern.matches(regex, instrumentName)) {
//                             String dateNow = df.format(dateTimeStart);
//                             String intrumentHash = instrumentName + dateNow;
//                             if (!intrumentSet.contains(intrumentHash)) {
//                                 intrumentSet.add(intrumentHash);
//                                 float markPrice = trade.getFloat("mark_price");
//                                 float iv = trade.getFloat("iv");
//                                 float indexPrice = trade.getFloat("index_price");
//
//                                 store2DB(points, instrumentName, dateNow, markPrice, iv, indexPrice);
//                                 store2Excel(optionDataList, instrumentName, dateNow, markPrice, iv, indexPrice);
//                             }
//                         }
//                     }//for
//
//                     JSONObject trade1 = (JSONObject) trades.get(trades.size() - 1);
//                     long timeStamp = (long) trade1.get("timestamp");
//                     dateTimeStart = LocalDateTime.ofEpochSecond(timeStamp / 1000, 0, ZoneOffset.UTC);
//                     //write to influxdb
//                     try{
//                         writeApi.writePoints(points);
//                     }catch (Exception ex){
//                         log.error("write to influxdb error", ex);
//                     }
//                 }//if
//             }//while
//
//             //write to excel
//             try{
//                 log.info("write to excel {}", i);
//                 excelWriter.write(optionDataList,writeSheet);
//             }catch (Exception ex){
//                 log.error("write to excel error:{}", ex);
//                 excelWriter.finish();
//             }
//             excelWriter.finish();
//             return "success";
//         }


//    public void getOptionInfoBak() throws InterruptedException {
//        Map<Integer,Map<String, String>> dateHandleMap = new HashMap<>();
//
//        Map<String, String> map25 = new HashMap<>();
//        map25.put("strDateTime","2021-12-30T00:00:01");
//        map25.put("strDateTimeEnd","2022-01-01T00:00:01");
//        dateHandleMap.put(25,map25);
//
//        ExecutorService es = Executors.newFixedThreadPool(20);
//
//        for(Integer i: dateHandleMap.keySet()){
//            LocalDateTime threadStartTime = LocalDateTime.parse(dateHandleMap.get(i).get("strDateTime"));
//            LocalDateTime threadEndTime = LocalDateTime.parse(dateHandleMap.get(i).get("strDateTimeEnd"));
//            es.submit(new OptionFetch(threadStartTime,threadEndTime,i));
//        }
//
//    }



//        private  void store2Excel(List<OptionData> optionDataList, String instrumentName, String dateNow, float markPrice, float iv, float indexPrice) {
//                OptionData optionData = new OptionData();
//                optionData.setDate(dateNow);
//                optionData.setInstrumentName(instrumentName);
//                optionData.setIv(String.valueOf(iv));
//                optionData.setMarkPrice(String.valueOf(markPrice));
//                optionData.setIndexPrice(String.valueOf(indexPrice));
//                optionDataList.add(optionData);
//    }
//
//    private  void store2DB(List<Point> points, String instrumentName, String dateNow, float markPrice, float iv, float indexPrice) {
//        Point point = Point.measurement("options")
//                .addTag("exchange", "deribt")
//                .addTag("symbol", "ETH")
//                .addTag("instrumentName", instrumentName)
//                .addTag("dateNow", dateNow)
//                .addField("iv", iv)
//                .addField("markPrice", markPrice)
//                .addField("indexPrice", indexPrice);
//        points.add(point);
//    }

//    @NotNull
//    private  List<List<String>> getHeadList() {
//        List<List<String>> headList = new ArrayList<>();
//        List<String> head0 = new ArrayList<>();
//        head0.add("date");
//        List<String> head1 = new ArrayList<>();
//        head1.add("instrument_name");
//        List<String> head2 = new ArrayList<>();
//        head2.add("iv");
//        List<String> head3 = new ArrayList<>();
//        head3.add("mark_price");
//        List<String> head4 = new ArrayList<>();
//        head4.add("index_price");
//        headList.add(head0);
//        headList.add(head1);
//        headList.add(head2);
//        headList.add(head3);
//        headList.add(head4);
//        return headList;
//    }

    public static void main(String[] args) {
//        String regex = ".*-(C|P)\\b";
//        String input = "ETH-24SEP21-6000-C";
//        String input1 = "ETH-24SEP21-6000-Ceee";
//
//        String input2 = "ETH-PERPETUAL";
//        String input3 = "ETH-25MAR22";
//
//        boolean isMatch = Pattern.matches(regex, input1);
//        System.out.println(isMatch);


        String strDateTime = "2021-04-01T00:00:01";
        String strDateTimeEnd = "2021-06-01T00:00:01";

//      1 按照天循环
//      2 获取每天第一次取的，所有的instruments,筛选出期权,算出初次价格的market_price，
//      3 存入mysql
        int threadNum = 20;
        LocalDateTime startTime = LocalDateTime.parse(strDateTime);
        LocalDateTime dateTimeEnd = LocalDateTime.parse(strDateTimeEnd);


    }
}

