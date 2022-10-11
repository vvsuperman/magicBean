//package com.furiousTidy.magicbean.influxdb;
//
//import okhttp3.OkHttpClient;
//import org.influxdb.BatchOptions;
//import org.influxdb.InfluxDB;
//import org.influxdb.InfluxDBFactory;
//import org.influxdb.dto.*;
//import org.springframework.retry.annotation.Backoff;
//import org.springframework.retry.annotation.Retryable;
//import org.springframework.stereotype.Component;
//
//import java.util.Map;
//import java.util.concurrent.TimeUnit;
//
//
//
//public class InfluxDbConnection {
//
//    private String userName;
//    private String password;
//    private String url;
//    private String token;
//    public String database;
//    private String retentionPolicy;
//    private String retentionPolicyTime;
//    private InfluxDB influxdb;
//    private BatchOptions batchOptions;
//
//    static OkHttpClient.Builder client = new OkHttpClient.Builder()
//            .readTimeout(1000,TimeUnit.SECONDS);
//
//
//    public InfluxDbConnection(String token, String url){
//
//        this.url = url;
//        this.token = token;
//
//
//    }
//
//    public InfluxDbConnection(String userName, String password, String url, String database, String retentionPolicy, String retentionPolicyTime, BatchOptions batchOptions) {
//        this.userName = userName;
//        this.password = password;
//        this.url = url;
//        this.database = database;
//        //默认数据保存策略为autogen
//        this.retentionPolicy = retentionPolicy == null || "".equals(retentionPolicy) ? "autogen" : retentionPolicy;
//        this.retentionPolicyTime = retentionPolicyTime == null || "".equals(retentionPolicyTime) ? "30d" : retentionPolicyTime;
//        this.batchOptions = batchOptions == null ? BatchOptions.DEFAULTS : batchOptions;
//        this.influxdb = buildInfluxDb();
//    }
//
//    public InfluxDB buildInfluxDb() {
//        if (influxdb == null) {
//            influxdb = InfluxDBFactory.connect(url, userName, password,client);
//            try {
//                createDatabase(this.database);
//            } catch (Exception e) {
//                System.out.println("create database error " + e.getMessage());
//            }
//
//            influxdb.setDatabase(this.database);
//        }
//        return influxdb;
//    }
//
//    /**
//     * 设置数据保存策略:retentionPolicy策略名 /database 数据库名/ DURATION 数据保存时限/REPLICATION副本个数/结尾 DEFAULT
//     * DEFAULT表示设为默认的策略
//     */
//    public void createRetentionPolicy() {
//        String command = String.format("CREATE RETENTION POLICY \"%s\" ON \"%s\" DURATION %s REPLICATION %s DEFAULT",
//                retentionPolicy, database, retentionPolicyTime, 1);
//        this.query(command);
//    }
//
//    /**
//     * 设置自定义保留策略
//     *
//     * @param policyName
//     * @param duration
//     * @param replication
//     * @param isDefault
//     */
//    public void createRetentionPolicy(String policyName, String duration, int replication, boolean isDefault) {
//        String command = String.format("CREATE RETENTION POLICY \"%s\" ON \"%s\" DURATION %s REPLICATION %s ", policyName,
//                database, duration, replication);
//        if (isDefault) {
//            command = command + " DEFAULT";
//        }
//        this.query(command);
//    }
//
//    /**
//     * 创建数据库
//     *
//     * @param database
//     */
//    public void createDatabase(String database) {
//        influxdb.query(new Query("CREATE DATABASE " + database));
//    }
//
//    /**
//     * 操作数据库
//     *
//     * @param command
//     * @return
//     */
//    public QueryResult query(String command) {
//        return influxdb.query(new Query(command, database));
//    }
//
//    /**
//     * 插入数据库
//     *
//     * @param measurement
//     * @param tags
//     * @param fields
//     */
//    public void insert(String measurement, Map<String, String> tags, Map<String, Object> fields) {
//        insert(measurement, tags, fields, 0, null);
//    }
//
//    public void insert(String measurement, Map<String, String> tags, Map<String, Object> fields, long time, TimeUnit timeUnit) {
//        Point.Builder builder = Point.measurement(measurement);
//        builder.time(System.currentTimeMillis(), TimeUnit.MILLISECONDS);
//        builder.tag(tags);
//        builder.fields(fields);
//        if (0 < time) {
//            builder.time(time, timeUnit);
//        }
//        System.out.println(("influxDB insert data:" + builder.build().toString()));
//        influxdb.write(database, retentionPolicy, builder.build());
//    }
//
//    @Retryable( maxAttempts = 500, backoff = @Backoff(delay = 2000, multiplier = 1.1))
//    public void batchInsert(BatchPoints batchPoints) {
//
//        influxdb.write(batchPoints);
//    }
//
//    /**
//     * 批量操作结束时手动刷新数据
//     */
//    public void flush() {
//        if (influxdb != null) {
//            influxdb.flush();
//        }
//    }
//
//    /**
//     * 如果调用了enableBatch,操作结束时必须调用disableBatch或者手动flush
//     */
//    public void enableBatch() {
//        if (influxdb != null) {
//            influxdb.enableBatch(this.batchOptions);
//        }
//    }
//
//    public void disableBatch() {
//        if (influxdb != null) {
//            influxdb.disableBatch();
//        }
//    }
//
//    /**
//     * 测试是否已正常连接
//     *
//     * @return
//     */
//    public boolean ping() {
//        boolean isConnected = false;
//        Pong pong;
//        try {
//            pong = influxdb.ping();
//            if (pong != null) {
//                isConnected = true;
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        return isConnected;
//    }
//}
//
