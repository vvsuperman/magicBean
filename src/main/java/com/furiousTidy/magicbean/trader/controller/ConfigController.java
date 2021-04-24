package com.furiousTidy.magicbean.trader.controller;


import com.binance.api.client.domain.account.request.CancelOrderRequest;
import com.binance.client.model.market.ExchangeInfoEntry;
import com.binance.client.model.trade.AccountInformation;
import com.furiousTidy.magicbean.config.BeanConfig;
import com.furiousTidy.magicbean.util.BeanConstant;
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

    @RequestMapping("getConfig")
    public @ResponseBody Map getConfig() throws Exception {
        Map<String,Object> rtMap = new HashMap<>();

        Object o = BeanConfig.class.newInstance();
        Map<String,Object> configMap = new HashMap<>();
        for (Field field : BeanConfig.class.getFields())
        {
            if(field.getName().equals("SECRET_KEY") || field.getName().equals("API_KEY")
                    || field.getName().equals("tradeInfoMap") || field.getName().equals("pairsTradeList")) continue;
            configMap.put(field.getName(),field.get(o));
        }

        Object o1 = BeanConstant.class.newInstance();
        Map<String,Object> constantMap = new HashMap<>();
        for (Field field : BeanConstant.class.getFields())
        {
            if(field.getName().equals("SECRET_KEY") || field.getName().equals("API_KEY")
                    || field.getName().equals("tradeInfoMap") || field.getName().equals("pairsTradeList")) continue;
            constantMap.put(field.getName(),field.get(o1));
        }

        rtMap.put("config",configMap);
        rtMap.put("constant", constantMap);
        return rtMap;
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
        }else if(f.getType().getName().contains("long") || f.getType().getName().contains("Long")){
            f.set(o,Long.valueOf(value));
            return "success";
        }else if(f.getType().getName().contains("String")){
            f.set(o,value);
            return "success";
        }

        return "failed";
    }
}
