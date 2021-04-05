package com.furiousTidy.magicbean.trader.controller;


import com.binance.api.client.domain.account.Account;
import com.binance.api.client.domain.account.request.CancelOrderRequest;
import com.binance.client.model.market.ExchangeInfoEntry;
import com.binance.client.model.trade.AccountInformation;
import com.furiousTidy.magicbean.config.BeanConfig;
import com.furiousTidy.magicbean.util.BinanceClient;
import com.furiousTidy.magicbean.util.MarketCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.yaml.snakeyaml.error.Mark;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

@Controller
@RequestMapping(path = "/account")
@Slf4j
public class AccountController {





    @Autowired
    AsyncTest asyncTest;


    @RequestMapping("testopen")
    public @ResponseBody String testConfig(){
        return BeanConfig.OPEN_PRICE_GAP.toString();
    }

    @RequestMapping("futureinfo")
    public @ResponseBody Map getFutureInfo(){
        AccountInformation accountInformation =  BinanceClient.futureSyncClient.getAccountInformation();
        BigDecimal availableBalance = accountInformation.getTotalWalletBalance();
        final TreeMap<String,BigDecimal> assetInfo = new TreeMap<>();
        accountInformation.getAssets().forEach(asset -> {
            if(!asset.getMaxWithdrawAmount().equals(BigDecimal.ZERO)){
                assetInfo.put(asset.getAsset(),asset.getMaxWithdrawAmount());
            }
        });

        final TreeMap<String, String> positionMap = new TreeMap<>();
        accountInformation.getPositions().stream().filter(position -> new BigDecimal(position.getPositionAmt()).compareTo(BigDecimal.ZERO)!=0).
                forEach(position -> positionMap.put(position.getSymbol(),position.getPositionAmt()));
        HashMap rtmap = new HashMap<>();
//        rtmap.put("balance",availableBalance);
        rtmap.put("asset",assetInfo);
        rtmap.put("position",positionMap);

        final TreeMap<String, String> spotBalanceMap = new TreeMap<>();
        BinanceClient.spotSyncClient.getAccount().getBalances().stream().filter(assetBalance -> new BigDecimal(assetBalance.getFree()).compareTo(BigDecimal.ZERO)>0)
                .forEach(assetBalance -> spotBalanceMap.put(assetBalance.getAsset(),assetBalance.getFree()));
        rtmap.put("spotBalance",spotBalanceMap);
        return rtmap;
    }

    @RequestMapping("spotbalance")
    public @ResponseBody String getSpotAssetUSDT(){
        return  MarketCache.spotBalanceCache.get("USDT").getFree();
    }

    @RequestMapping("futurebalance")
    public @ResponseBody String getFutureAssetUSDT(){
        return MarketCache.futureBalanceCache.get("USDT").getWalletBalance().toString();
    }

    @RequestMapping("cancelallorder")
    public @ResponseBody void cancelallorder() {
        for (Map.Entry<String, ExchangeInfoEntry> entry : MarketCache.futureInfoCache.entrySet()) {
            BinanceClient.futureSyncClient.cancelAllOpenOrder(entry.getKey());
            BinanceClient.spotSyncClient.cancelOrder(new CancelOrderRequest(entry.getKey(),""));
        }
    }
}
