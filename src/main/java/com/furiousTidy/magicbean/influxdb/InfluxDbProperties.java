package com.furiousTidy.magicbean.influxdb;

import lombok.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "spring.influx")
public class InfluxDbProperties {

    private String url;
    private String userName;
    private String password;
    private String database;
    private String company;
    private String token;
    private String bucket;
    private String retentionPolicy = "autogen";
    private String retentionPolicyTime = "30d";
    private int actions = 2000;
    private int flushDuration = 1000;
    private int jitterDuration = 0;
    private int bufferLimit = 10000;

    public InfluxDbProperties() {
    }

    protected boolean canEqual(final Object other) {
        return other instanceof InfluxDbProperties;
    }

}


