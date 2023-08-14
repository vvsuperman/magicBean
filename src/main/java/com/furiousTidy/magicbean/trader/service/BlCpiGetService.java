package com.furiousTidy.magicbean.trader.service;

import com.furiousTidy.magicbean.config.BeanConfig;
import com.furiousTidy.magicbean.util.BeanConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;

@Slf4j
@Service
public class BlCpiGetService {

    static volatile float blCpi =0;

    //String blsUrl = "https://www.bls.gov/cpi/";
    String blsUrl = "https://www.bls.gov/cpi/latest-numbers.htm";
    @Autowired
    RestTemplate restTemplate;

    @Async
    public void getCPIfromBlsLoop( LocalDateTime newsTime)  {
        while (true){
          if(getCPIfromBls(  newsTime)!=0){
              return;
          }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                log.error("getCPIfromBlsLoop error...." + e);
            }
        }
    }


    public float getCPIfromBls(LocalDateTime newsTime){

        int monthInt  = newsTime.getMonth().getValue();
        String month = BeanConstant.MONTH_LIST.get(monthInt-2);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/json");
        HttpEntity entity = new HttpEntity(headers);

        log.info("get bls data begin: " + LocalDateTime.now());
        String responseEntity="";
        try{
            responseEntity = restTemplate.exchange(blsUrl, HttpMethod.GET,entity, String.class).getBody();
            if( responseEntity == null){
                return 0;
            }
            return getCPIfromText(responseEntity, month);
        }catch (Exception ex){
            log.error("get url or cpi exception: {}", ex);
        }
        return 0;
    }

    float getCPIfromText(String responseEntity,String month) throws Exception {
        int index = responseEntity.indexOf("%", (responseEntity.indexOf("12-month percent change, not seasonally adjusted") + 1));
        String cpiValue = responseEntity.substring(index - 3, index);
        int periodIndex = responseEntity.indexOf("period-text");
        String cpiMonth = responseEntity.substring(periodIndex+13,periodIndex+16);
        if( cpiMonth.toUpperCase().equals(month)){
            return Float.parseFloat(cpiValue);
        }else{
            return 0;
        }
    }
}
