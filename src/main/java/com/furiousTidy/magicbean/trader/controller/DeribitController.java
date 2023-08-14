package com.furiousTidy.magicbean.trader.controller;

import com.furiousTidy.magicbean.trader.service.DeribitService;
import com.furiousTidy.magicbean.trader.service.MarketService;
import com.furiousTidy.magicbean.util.TradeUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping(path = "/deribit")
@Slf4j
public class DeribitController {

   @Autowired
    DeribitService deribitService;

   @Autowired
   TradeUtil tradeUtil;


    @RequestMapping("getOptionHisData")
    public @ResponseBody String getHisData() throws InterruptedException {
        try {
            deribitService.getOptionInfo();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "getHisDeribitData";
    }



}
