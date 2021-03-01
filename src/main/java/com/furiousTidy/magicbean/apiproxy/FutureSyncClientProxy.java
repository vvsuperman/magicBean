package com.furiousTidy.magicbean.apiproxy;

import com.binance.client.model.market.ExchangeInformation;
import com.binance.client.model.market.SymbolOrderBook;
import com.furiousTidy.magicbean.util.BinanceClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class FutureSyncClientProxy {

    @Retryable( maxAttempts = 5, backoff = @Backoff(delay = 2000, multiplier = 1.5))
    public ExchangeInformation getExchangeInfo(){
        log.info("try to get spot exchangeinfo");
        return BinanceClient.futureSyncClient.getExchangeInformation();
    }

    @Retryable( maxAttempts = 5, backoff = @Backoff(delay = 2000, multiplier = 1.5))
    public List<SymbolOrderBook> getAllBookTickers(){
        return BinanceClient.futureSyncClient.getSymbolOrderBookTicker(null);
    }
}
