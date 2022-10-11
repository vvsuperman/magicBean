package com.furiousTidy.magicbean.config;


import com.furiousTidy.magicbean.influxdb.InfluxDbProperties;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;

@Configuration
@Service
public class InfluxDBConfig {

    @Autowired
    InfluxDbProperties influxDbProperties;

    @Bean
    public InfluxDBClient buildConnection() {

        return InfluxDBClientFactory.create(influxDbProperties.getUrl(), influxDbProperties.getToken().toCharArray(),
                influxDbProperties.getCompany(), influxDbProperties.getBucket());
    }

}
