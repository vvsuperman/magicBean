//package com.furiousTidy.magicbean.config;
//
//
//import com.furiousTidy.magicbean.influxdb.InfluxDbConnection;
//import com.furiousTidy.magicbean.influxdb.InfluxDbProperties;
//import org.influxdb.BatchOptions;
//import org.influxdb.InfluxDB;
//import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
//import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
//import org.springframework.boot.context.properties.ConfigurationProperties;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.stereotype.Service;
//
//
//@Configuration
//@ConditionalOnClass(InfluxDB.class)
//@Service
//public class InfluxDBConfiguration {
//
//    @Bean
//    @ConfigurationProperties(prefix = "spring.influx")
//    @ConditionalOnMissingBean(name = "influxDbProperties")
//    InfluxDbProperties influxDbProperties() {
//        return new InfluxDbProperties();
//    }
//
//
//    @Bean
//    public InfluxDB
//
//
//    @Bean
//    @ConditionalOnMissingBean(name = "influxDbConnection")
//    public InfluxDbConnection influxDbConnection(InfluxDbProperties influxDbProperties) {
//        BatchOptions batchOptions = BatchOptions.DEFAULTS;
//        batchOptions = batchOptions.actions(influxDbProperties.getActions());
//        batchOptions = batchOptions.flushDuration(influxDbProperties.getFlushDuration());
//        batchOptions = batchOptions.jitterDuration(influxDbProperties.getJitterDuration());
//        batchOptions = batchOptions.bufferLimit(influxDbProperties.getBufferLimit());
//
//        InfluxDbConnection influxDbConnection = new InfluxDbConnection(influxDbProperties.getUserName(), influxDbProperties.getPassword(),
//                influxDbProperties.getUrl(), influxDbProperties.getDatabase(), influxDbProperties.getRetentionPolicy(),
//                influxDbProperties.getRetentionPolicyTime(), batchOptions);
//        influxDbConnection.createRetentionPolicy();
//
//        System.out.println("init influxDb >>>>>>" + influxDbProperties);
//        return influxDbConnection;
//    }
//}
//
