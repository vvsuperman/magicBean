package com.furiousTidy.magicbean.trader.controller;


import com.binance.api.client.domain.account.request.CancelOrderRequest;
import com.binance.client.model.market.ExchangeInfoEntry;
import com.binance.client.model.trade.AccountInformation;
import com.furiousTidy.magicbean.config.BeanConfig;
import com.furiousTidy.magicbean.util.BinanceClient;
import com.furiousTidy.magicbean.util.MarketCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

@Controller
@RequestMapping(path = "/dynamicvalue")
@Slf4j
public class ConfigController {

    @RequestMapping("testopen")
    public @ResponseBody String testConfig(){
        return BeanConfig.ORDER_EXPIRE_TIME+"";
    }

    @RequestMapping("change/{key}/{value}")
    public @ResponseBody  String modifyConfig(@PathVariable String key, @PathVariable String value) throws NoSuchFieldException, IllegalAccessException, InstantiationException {
        Object o = BeanConfig.class.newInstance();
        Field f = BeanConfig.class.getField(key);
        if(f.getType().getName().contains("BigDecimal")){
            f.set(o,new BigDecimal(value));
            return "success";
        }else if(f.getType().getName().contains("int")){
            f.set(o,Integer.valueOf(value));
            return "success";
        }else if(f.getType().getName().contains("long")){
            f.set(o,Long.valueOf(value));
            return "success";
        }else if(f.getType().getName().contains("String")){
            f.set(o,value);
            return "success";
        }

        return "failed";
    }
}
