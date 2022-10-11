package com.furiousTidy.magicbean.trader.controller;


import com.binance.api.client.domain.account.request.CancelOrderRequest;
import com.binance.client.model.market.ExchangeInfoEntry;
import com.furiousTidy.magicbean.config.BeanConfig;
import com.furiousTidy.magicbean.trader.service.TradeScheduleService;
import com.furiousTidy.magicbean.trader.service.TradeHelpService;
import com.furiousTidy.magicbean.util.BinanceClient;
import com.furiousTidy.magicbean.util.MarketCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;

@Controller
@RequestMapping(path = "/account")
@Slf4j
public class AccountController {





    @Autowired
    AsyncTest asyncTest;

    @Autowired
    TradeScheduleService tradeScheduleService;

    @Autowired
    BinanceClient binanceClient;

    @Autowired
    TradeHelpService tradeHelpService;


    @RequestMapping("testopen")
    public @ResponseBody String testConfig(){
        return BeanConfig.OPEN_PRICE_GAP.toString();
    }

    @RequestMapping("info")
    public @ResponseBody Map getBalanceInfo(){
       Map rtmap =  tradeHelpService.getBalanceInfo();

        rtmap.put("totalBalance",tradeScheduleService.getAllBalance());

        return rtmap;

    }

//    @RequestMapping("spotbalance")
//    public @ResponseBody String getSpotAssetUSDT(){
//        return  MarketCache.spotBalanceCache.get("USDT").getFree();
//    }
//
//    @RequestMapping("futurebalance")
//    public @ResponseBody String getFutureAssetUSDT(){
//        return MarketCache.futureBalanceCache.get("USDT").getWalletBalance().toString();
//    }

    @RequestMapping("cancelallorder")
    public @ResponseBody void cancelallorder() {
        for (Map.Entry<String, ExchangeInfoEntry> entry : MarketCache.futureInfoCache.entrySet()) {
            binanceClient.getFutureSyncClient().cancelAllOpenOrder(entry.getKey());
            binanceClient.getSpotSyncClient().cancelOrder(new CancelOrderRequest(entry.getKey(),""));
        }
    }
}
