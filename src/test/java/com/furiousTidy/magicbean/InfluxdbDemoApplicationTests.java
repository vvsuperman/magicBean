//package com.furiousTidy.magicbean;
//
////import com.furiousTidy.magicbean.influxdb.InfluxDbConnection;
//import org.influxdb.dto.QueryResult;
//import org.junit.Test;
//import org.junit.runner.RunWith;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.test.context.junit4.SpringRunner;
//
//import javax.annotation.Resource;
//import java.util.*;
//import java.util.stream.Collectors;
//
//@RunWith(SpringRunner.class)
//@SpringBootTest
//public class InfluxdbDemoApplicationTests {
//    //需要使用的地方直接注入
////    @Autowired
////    InfluxDbConnection influxDBConnection;
//
//    @Test
//    public void contextLoads() {
//        System.out.println("Test Start");
//    }
//
//    @Test
//    public void testInsert() {
//        //准备数据
//        int num = 200;
//        List<Map<String, String>> tagsList = new ArrayList<>();
//        List<Map<String, Object>> fieldsList = new ArrayList<>();
//        for (int i = 0; i < num; i++) {
//            Map<String, String> tagsMap = new HashMap<>();
//            Map<String, Object> fieldsMap = new HashMap<>();
//            tagsMap.put("driver_id", "D" + i);
//            tagsMap.put("mobile", "1812930821" + i);
//            fieldsMap.put("totalDistance", (new Random().nextInt(900) + 100));
//            tagsList.add(tagsMap);
//            fieldsList.add(fieldsMap);
//        }
//
//        //插入数据
//        long start = System.currentTimeMillis();
//        influxDBConnection.enableBatch();
//        for (int k = 0; k < tagsList.size(); k++) {
//            influxDBConnection.insert("driverStatistics", tagsList.get(k), fieldsList.get(k));
//        }
//        influxDBConnection.disableBatch();
//        long end = System.currentTimeMillis();
//        System.out.println("cost:" + (end - start));
//
//        //查询数据
//        //QueryResult query = influxDBConnection.query("select count(*) from driverStatistics");
//        QueryResult query = influxDBConnection.query("select * from driverStatistics where driver_id=" + "\'D1\'");
//
//        List<QueryResult.Result> results = query.getResults();
//        //只有一条查询语句取第一条查询结果即可
//        QueryResult.Result result = results.get(0);
//        List<Map<String, String>> res = new ArrayList<>();
//        if (result.getSeries() != null) {
//            List<List<Object>> valueList = result.getSeries().stream().map(QueryResult.Series::getValues)
//                    .collect(Collectors.toList()).get(0);
//            if (valueList != null && valueList.size() > 0) {
//                for (List<Object> value : valueList) {
//                    Map<String, String> resMap = new HashMap<>();
//                    // 查询结果字段1取值
//                    String field1 = value.get(0) == null ? null : value.get(0).toString();
//                    resMap.put("time", field1);
//                    // 查询结果字段2取值
//                    String field2 = value.get(1) == null ? null : value.get(1).toString();
//                    resMap.put("driver_id", field2);
//                    // 查询结果字段3取值
//                    String field3 = value.get(2) == null ? null : value.get(2).toString();
//                    resMap.put("mobile", field3);
//                    // 查询结果字段4取值
//                    String field4 = value.get(3) == null ? null : value.get(3).toString();
//                    resMap.put("totalDistance", field4);
//
//                    res.add(resMap);
//                }
//            }
//        }
//
//        System.out.println(res);
//        System.out.println("-----------------------------------------------------------");
//
//        QueryResult queryOne = influxDBConnection.query("select * from driverStatistics where driver_id=" + "\'D1\'" + " and time=" + "1594636413114000000");
//        List<QueryResult.Series> series = queryOne.getResults().get(0).getSeries();
//        Map<String, String> oneRes = new HashMap<>();
//        if (series != null) {
//            List<List<Object>> valueList = series.get(0).getValues();
//            for (List<Object> value : valueList) {
//                // 查询结果字段1取值
//                String field1 = value.get(0) == null ? null : value.get(0).toString();
//                oneRes.put("time", field1);
//                // 查询结果字段2取值
//                String field2 = value.get(1) == null ? null : value.get(1).toString();
//                oneRes.put("driver_id", field2);
//                // 查询结果字段3取值
//                String field3 = value.get(2) == null ? null : value.get(2).toString();
//                oneRes.put("mobile", field3);
//                // 查询结果字段4取值
//                String field4 = value.get(3) == null ? null : value.get(3).toString();
//                oneRes.put("totalDistance", field4);
//            }
//        }
//
//        System.out.println(oneRes);
//    }
//}
