package com.furiousTidy.magicbean.trader.controller;

import com.binance.client.model.enums.OrderSide;
import com.furiousTidy.magicbean.config.BeanConfig;
import com.furiousTidy.magicbean.trader.TradeDto.JinShiDto;
import com.furiousTidy.magicbean.trader.service.NewsStrategyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
@RequestMapping(path = "/news")
@Slf4j
public class NewsController {

    @Autowired
    NewsStrategyService newsStrategyService;

    @RequestMapping("do")
    public @ResponseBody void doNews() throws InterruptedException {
        BeanConfig.NEW_STOP_FLAG = false;
        newsStrategyService.doNewsStrategy();
    }

    @RequestMapping("stopdo")
    public @ResponseBody void stopDoNews() throws InterruptedException {
        BeanConfig.NEW_STOP_FLAG = true;
    }

    @RequestMapping("getNewsList")
    public @ResponseBody List<JinShiDto> getNewsList() throws InterruptedException {
        return newsStrategyService.getJinShiDtoList();
    }




    @RequestMapping("testOrder")
    public @ResponseBody void testOrder(){
        newsStrategyService.doNewsStrategyOrder(OrderSide.BUY);
    }


    @RequestMapping("testGetName")
    public @ResponseBody void testGetName(){
        newsStrategyService.getInfoThenOrder();
    }
}
