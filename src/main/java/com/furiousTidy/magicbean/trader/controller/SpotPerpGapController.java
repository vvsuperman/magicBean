package com.furiousTidy.magicbean.trader.controller;

import com.furiousTidy.magicbean.subscription.MxSub;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping(path = "/spotPerpGap")
@Slf4j
public class SpotPerpGapController {

    @Autowired
    MxSub mxSub;

    @RequestMapping("getPerpTrade")
    public @ResponseBody String doCompare() throws Exception {
        try {
            mxSub.getSpotPerpInfo();
            mxSub.subAllSpotTrade();

        } catch (Exception e) {
            e.printStackTrace();
        }
        return "sub all spot perp sucess";
    }
}
