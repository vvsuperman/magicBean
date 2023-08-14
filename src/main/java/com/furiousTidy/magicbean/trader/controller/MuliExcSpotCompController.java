package com.furiousTidy.magicbean.trader.controller;

import com.furiousTidy.magicbean.config.BeanConfig;
import com.furiousTidy.magicbean.subscription.SpotSubscription;
import com.furiousTidy.magicbean.trader.service.BlCpiGetService;
import com.furiousTidy.magicbean.trader.service.MulteExcSpotCompService;
import com.furiousTidy.magicbean.trader.service.NewsStrategyServiceByPrice;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.LocalDateTime;

@Controller
@RequestMapping(path = "/muliExcSpotComp")
@Slf4j
public class MuliExcSpotCompController {

    @Autowired
    MulteExcSpotCompService multeExcSpotCompService;

    @Autowired
    SpotSubscription spotSubscription;

    @RequestMapping("doCompare")
    public @ResponseBody String doCompare() throws Exception {
        try {
            multeExcSpotCompService.doCompare();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "getKLineTest";
    }

    @RequestMapping("subAllBNTrade")
    public @ResponseBody String subAllBNTrade() throws Exception {
        try {
            spotSubscription.subAllTrade();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "getKLineTest";
    }


}
