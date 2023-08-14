package com.furiousTidy.magicbean.apiproxy;


import com.alibaba.fastjson.JSONObject;
import com.binance.api.client.domain.account.NewOrder;
import com.binance.api.client.domain.account.NewOrderResponse;
import com.binance.api.client.domain.general.ExchangeInfo;
import com.binance.api.client.domain.market.BookTicker;
import com.furiousTidy.magicbean.config.BeanConfig;
import com.furiousTidy.magicbean.util.BeanConstant;
import com.furiousTidy.magicbean.util.BinanceClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.SocketTimeoutException;
import java.util.List;

/**
 * @author CloudUREE004829
 *
 */
@Service
@Slf4j
public class DeribitClientProxy {

    @Autowired
    RestTemplate restTemplate;

    @Retryable(value={SocketTimeoutException.class}, maxAttempts = 10, backoff = @Backoff(delay = 10000, multiplier = 1.5))
    public ResponseEntity<JSONObject> getOptionInfo(String url){
        HttpHeaders headers = new HttpHeaders();
        HttpEntity entity = new HttpEntity(headers);
       return  restTemplate.exchange(url, HttpMethod.GET, entity, JSONObject.class);
    }


}
