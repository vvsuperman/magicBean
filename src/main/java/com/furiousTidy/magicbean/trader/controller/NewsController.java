package com.furiousTidy.magicbean.trader.controller;

import com.binance.client.model.enums.OrderSide;
import com.furiousTidy.magicbean.config.BeanConfig;
import com.furiousTidy.magicbean.trader.TradeDto.JinShiDto;
import com.furiousTidy.magicbean.trader.service.BlCpiGetService;
import com.furiousTidy.magicbean.trader.service.NewsStrategyService;
import com.furiousTidy.magicbean.trader.service.NewsStrategyServiceByPrice;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping(path = "/news")
@Slf4j
public class NewsController {

    @Autowired
    NewsStrategyServiceByPrice newsStrategyServiceByPrice;

    @Autowired
    BlCpiGetService blCpiGetService;



    @RequestMapping("do")
    public @ResponseBody String doNews() throws InterruptedException {
        log.info("新闻策略开始，请打开缓存获取tick信息");
        BeanConfig.NEW_STOP_FLAG = false;
        newsStrategyServiceByPrice.doNewsStrategy();
        return "新闻策略开始，请打开缓存获取tick信息";
    }

    @RequestMapping("stopdo")
    public @ResponseBody void stopDoNews() throws InterruptedException {
        BeanConfig.NEW_STOP_FLAG = true;
    }




    @RequestMapping("testCpi")
    public @ResponseBody String  doNewsTest() throws InterruptedException {
        return String.valueOf(blCpiGetService.getCPIfromBls(LocalDateTime.now()));
    }


//    @RequestMapping("getNewsList")
//    public @ResponseBody List<JinShiDto> getNewsList() throws InterruptedException {
//        return newsStrategyService.getJinShiDtoList();
//    }
//    @RequestMapping("testOrder")
//    public @ResponseBody void testOrder(){
//        newsStrategyService.doNewsStrategyOrder(OrderSide.BUY);
//    }
//
//
//    @RequestMapping("testGetName")
//    public @ResponseBody void testGetName(){
//        newsStrategyService.getInfoThenOrder();
//    }
}
