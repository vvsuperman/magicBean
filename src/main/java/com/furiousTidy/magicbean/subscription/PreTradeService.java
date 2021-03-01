package com.furiousTidy.magicbean.subscription;

import com.binance.client.model.market.ExchangeInformation;
import com.furiousTidy.magicbean.apiproxy.FutureSyncClientProxy;
import com.furiousTidy.magicbean.apiproxy.SpotSyncClientProxy;
import com.furiousTidy.magicbean.util.BinanceClient;
import com.furiousTidy.magicbean.util.MarketCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class PreTradeService {

    @Autowired
    SpotSyncClientProxy spotSyncClientProxy;

    @Autowired
    FutureSyncClientProxy futureSyncClientProxy;

    //get exchange info for future
    public void futureExchangeInfo(){
       ExchangeInformation exchangeInfo = null;
       try {
           exchangeInfo=  futureSyncClientProxy.getExchangeInfo();
       }catch (Exception ex){
            log.error("get future Exchange exception:{}",ex);
       }

       exchangeInfo.getSymbols().forEach(exchangeInfoEntry -> {
           MarketCache.futureInfoCache.put(exchangeInfoEntry.getSymbol(),exchangeInfoEntry);
       });
    }

    //get exchange info for spot
    public void spotExchangeInfo(){
        try{
            spotSyncClientProxy.getExchangeInfo().getSymbols().forEach(symbolInfo -> {
                MarketCache.spotInfoCache.put(symbolInfo.getSymbol(),symbolInfo);
            });
        }catch(Exception ex){
            log.error("get spot exchange info error:{}",ex);
        }

    }
}
