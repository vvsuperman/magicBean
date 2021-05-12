package com.binance.api.examples;

import com.binance.api.client.BinanceApiClientFactory;
import com.binance.api.client.BinanceApiRestClient;
import com.binance.api.client.constant.SpotConstants;
import com.binance.api.client.domain.TimeInForce;
import com.binance.api.client.domain.account.NewOrderResponse;
import com.binance.api.client.domain.account.NewOrderResponseType;
import com.binance.api.client.domain.account.Order;
import com.binance.api.client.domain.account.request.*;
import com.binance.api.client.domain.market.BookTicker;
import com.binance.api.client.exception.BinanceApiException;

import java.util.List;

import static com.binance.api.client.domain.account.NewOrder.limitBuy;
import static com.binance.api.client.domain.account.NewOrder.marketBuy;

/**
 * Examples on how to place orders, cancel them, and query account information.
 */
public class BookTickerExample {

  public static void main(String[] args) {
    BinanceApiClientFactory factory = BinanceApiClientFactory.newInstance(SpotConstants.REAL_API_KEY, SpotConstants.REAL_API_SECRET);
    BinanceApiRestClient client = factory.newRestClient();

    BookTicker bookTicker = client.getBookTicker("BTCUSDT");
    System.out.println(bookTicker);
  }

}
