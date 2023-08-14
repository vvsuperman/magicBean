package com.furiousTidy.magicbean.trader.controller;

import com.furiousTidy.magicbean.subscription.MxSub;
import com.furiousTidy.magicbean.trader.service.MarketService;
import com.furiousTidy.magicbean.util.TradeUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping(path = "/marketInfo")
@Slf4j
public class MarketController {

   @Autowired
   MarketService marketService;

   @Autowired
   TradeUtil tradeUtil;

   @Autowired
   MxSub mxSub;


    @RequestMapping("subMxTrade")
    public @ResponseBody String subMxTrade() throws InterruptedException {
        try {
            mxSub.subAllSpotTrade();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "getKLineTest";
    }
    @RequestMapping("getKLineHis")
    public @ResponseBody String getKLineHis() throws InterruptedException {
        try {
            marketService.getAndStoreKLineHis();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "getKLineTest";
    }



    @RequestMapping("getKLineTest")
    public @ResponseBody String getKLineTest() throws InterruptedException {
        try {
            marketService.getKlineTest();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "getKLineTest";
    }


    @RequestMapping("getDvol")
    public @ResponseBody String getDvol() throws InterruptedException {
        try {
            marketService.getDvol();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "getDvol";
    }


    @RequestMapping("getAndStoreAllKLine")
    public @ResponseBody String justTest() throws InterruptedException {
        marketService.getAndStoreAllKLine();
        return "getAndStoreAllKLine";
    }


    @RequestMapping("getGlobalLongShortAccountRatio")
    public @ResponseBody String justTest2() throws InterruptedException {
        try {
            marketService.getGlobalLongShortAccountRatio();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "getGlobalLongShortAccountRatio";
    }


   @RequestMapping("getOpenInterest")
    public @ResponseBody String getOpenInterest(

   ) throws InterruptedException {

       marketService.getOpenInterest();
       return "success";
   }

   @RequestMapping("alertOpenInterest")
    public @ResponseBody String alertOpenInterest(){
       try {
           marketService.alertOpenInterest();
       } catch (Exception e) {
           e.printStackTrace();
       }
       return "success";
   }


//    @RequestMapping("get")
//    public @ResponseBody String getAndStoreAllKLine(){
//
//        marketService.getOpenInterestHis();
//        return "success";
//    }


    @RequestMapping("getOpenInterestHis")
    public @ResponseBody String
    getOpenInterestHis(
            @PathVariable(name="startTime",required = false) Long startTime,
            @PathVariable(name="endTime", required = false)  Long endTime,
            @PathVariable(name="limit", required = false) Integer limit
    ){

        marketService.getOpenInterestHis(startTime, endTime, limit);
        return "success";
    }




}
