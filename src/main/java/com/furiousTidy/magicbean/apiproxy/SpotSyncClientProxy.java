package com.furiousTidy.magicbean.apiproxy;


import com.binance.api.client.domain.account.NewOrder;
import com.binance.api.client.domain.account.NewOrderResponse;
import com.binance.api.client.domain.general.ExchangeInfo;
import com.binance.api.client.domain.market.BookTicker;
import com.furiousTidy.magicbean.config.BeanConfig;
import com.furiousTidy.magicbean.util.BeanConstant;
import com.furiousTidy.magicbean.util.BinanceClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.net.SocketTimeoutException;
import java.util.List;

/**
 * @author CloudUREE004829
 *
 */
@Service
@Slf4j
public class SpotSyncClientProxy {

    @Autowired
    ProxyUtil proxyUtil;

    @Autowired
    BinanceClient binanceClient;

    @Retryable(value={SocketTimeoutException.class}, maxAttempts = 5, backoff = @Backoff(delay = 2000, multiplier = 1.5))
    public ExchangeInfo getExchangeInfo(){
        log.info("try to get spot exchangeinfo");
       return  binanceClient.getSpotSyncClient().getExchangeInfo();
    }

    @Retryable(value={SocketTimeoutException.class},maxAttempts = 5, backoff = @Backoff(delay = 2000, multiplier = 1.5))
    public NewOrderResponse newOrder(NewOrder newOrder){
        long start = System.currentTimeMillis();

        //make order
        NewOrderResponse order =  binanceClient.getSpotSyncClient().newOrder(newOrder);

        // adjust balance
//        proxyUtil.addBalance(BeanConfig.STANDARD_TRADE_UNIT.subtract(new BigDecimal(order.getFills().get(0).getPrice()).multiply(new BigDecimal(order.getFills().get(0).getQty()))),"spot");

        //check if the order cost too long
        long duration = System.currentTimeMillis() - start;
        if(duration > 50){
            BeanConstant.NETWORK_DELAYED = true;
            new Thread(() -> {
                try {
                    log.info("spot network is too slow, stop trade, duration={}",duration);
                    Thread.sleep(BeanConfig.NET_DELAY_TIME);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    BeanConstant.NETWORK_DELAYED = false;
                }
            }).start();
        }
        return order;
    }

    @Retryable(value={SocketTimeoutException.class},maxAttempts = 5, backoff = @Backoff(delay = 2000, multiplier = 1.5))
    public BookTicker getBookTicker(String symbol){
        return binanceClient.getSpotSyncClient().getBookTicker(symbol);
    }

    @Retryable(value={SocketTimeoutException.class},maxAttempts = 5, backoff = @Backoff(delay = 2000, multiplier = 1.5))
    public List<BookTicker> getAllBookTickers(){
        return binanceClient.getSpotSyncClient().getBookTickers();
    }


    public static void main(String[] args){
        new Thread(() -> {
            try {
                log.info("doing 1");
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                log.info("doing 2");
            }

        }).start();
    }
}
