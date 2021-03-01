package com.furiousTidy.magicbean.apiproxy;


import com.binance.api.client.domain.account.NewOrder;
import com.binance.api.client.domain.account.NewOrderResponse;
import com.binance.api.client.domain.general.ExchangeInfo;
import com.binance.api.client.domain.market.BookTicker;
import com.furiousTidy.magicbean.util.BinanceClient;
import com.sun.xml.internal.fastinfoset.util.ValueArrayResourceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.net.SocketTimeoutException;
import java.util.List;

/**
 * @author CloudUREE004829
 *
 */
@Service
@Slf4j
public class SpotSyncClientProxy {

    @Retryable(value={SocketTimeoutException.class}, maxAttempts = 5, backoff = @Backoff(delay = 2000, multiplier = 1.5))
    public ExchangeInfo getExchangeInfo(){
        log.info("try to get spot exchangeinfo");
       return  BinanceClient.spotSyncClient.getExchangeInfo();
    }

    @Retryable(value={SocketTimeoutException.class},maxAttempts = 5, backoff = @Backoff(delay = 2000, multiplier = 1.5))
    public NewOrderResponse newOrder(NewOrder newOrder){
        return BinanceClient.spotSyncClient.newOrder(newOrder);
    }

    @Retryable(value={SocketTimeoutException.class},maxAttempts = 5, backoff = @Backoff(delay = 2000, multiplier = 1.5))
    public BookTicker getBookTicker(String symbol){
        return BinanceClient.spotSyncClient.getBookTicker(symbol);
    }

    @Retryable(value={SocketTimeoutException.class},maxAttempts = 5, backoff = @Backoff(delay = 2000, multiplier = 1.5))
    public List<BookTicker> getAllBookTickers(){
        return BinanceClient.spotSyncClient.getBookTickers();
    }
}
