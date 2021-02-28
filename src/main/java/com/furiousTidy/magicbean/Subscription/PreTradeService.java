package com.furiousTidy.magicbean.Subscription;

import com.binance.client.model.market.ExchangeInformation;
import com.furiousTidy.magicbean.util.BinanceClient;
import com.furiousTidy.magicbean.util.MarketCache;
import org.springframework.stereotype.Service;

@Service
public class PreTradeService {

    //获取交易所信息
    public void futureExchangeInfo(){
       ExchangeInformation exchangeInfo =  BinanceClient.futureSyncClient.getExchangeInformation();
       exchangeInfo.getSymbols().forEach(exchangeInfoEntry -> {
           MarketCache.futureInfoCache.put(exchangeInfoEntry.getSymbol(),exchangeInfoEntry);
       });
    }

    //get exchange info for spot
    public void spotExchangeInfo(){
        BinanceClient.spotSyncClient.getExchangeInfo().getSymbols().forEach(symbolInfo -> {
            MarketCache.spotInfoCache.put(symbolInfo.getSymbol(),symbolInfo);
        });
    }
}
