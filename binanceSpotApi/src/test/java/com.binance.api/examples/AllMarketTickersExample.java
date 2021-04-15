package com.binance.api.examples;

import com.binance.api.client.BinanceApiClientFactory;
import com.binance.api.client.BinanceApiWebSocketClient;
import com.binance.api.client.constant.SpotConstants;
import com.binance.api.client.domain.event.AggTradeEvent;
import com.binance.api.client.domain.event.CandlestickEvent;
import com.binance.api.client.domain.market.CandlestickInterval;

import java.util.Objects;

/**
 * All market tickers channel examples.
 *
 * It illustrates how to create a stream to obtain all market tickers.
 */
public class AllMarketTickersExample {

  public static void main(String[] args) {
    BinanceApiClientFactory factory = BinanceApiClientFactory.newInstance();
    BinanceApiWebSocketClient client = factory.newWebSocketClient();

//    client.onAggTradeEvent("ethbtc", (AggTradeEvent response) -> {
//          System.out.println(response.getPrice());
//          System.out.println(response.getQuantity());
//      });

    client.onAllMarketTickersEvent(event -> {
      System.out.println(event);
    });

//    client.onCandlestickEvent("ethbtc,ethusdt", CandlestickInterval.ONE_MINUTE, (CandlestickEvent response) -> {
//      if (Objects.equals(response.getSymbol(),"ethbtc")) {
//        // handle ethbtc event
//        System.out.println(response);
//
//      } else if(Objects.equals(response.getSymbol(),"ethusdt")) {
//        // handle ethusdt event
//        System.out.println(response);
//
//      }
//    });
  }
}
