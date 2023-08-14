package com.furiousTidy.magicbean.trader.controller;

import com.furiousTidy.magicbean.config.BeanConfig;
import com.furiousTidy.magicbean.trader.service.BlCpiGetService;
import com.furiousTidy.magicbean.trader.service.NewPerpToSpotService;
import com.furiousTidy.magicbean.trader.service.NewsStrategyServiceByPrice;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.LocalDateTime;

@Controller
@RequestMapping(path = "/perp2Spot")
@Slf4j
public class NewPerp2SpotController {
    @Autowired
    NewPerpToSpotService newPerpToSpotService;


    @RequestMapping("doPerp2Spot")
    public @ResponseBody String doNew2Spot() throws InterruptedException {
        log.info("合约现货策略开始");
        newPerpToSpotService.doSpot();
        return "合约现货策略开始，请打开缓存获取tick信息";
    }
}
