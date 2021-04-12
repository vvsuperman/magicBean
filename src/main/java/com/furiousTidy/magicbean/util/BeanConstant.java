package com.furiousTidy.magicbean.util;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class BeanConstant {

    public static boolean NETWORK_DELAYED = true;
    public static boolean watchdog = true;

    public static final AtomicBoolean HAS_NEW_TRADE_OPEN = new AtomicBoolean(false);

    public static final AtomicBoolean ENOUGH_MONEY= new AtomicBoolean(true);

    public static final String BEST_ASK_PRICE="bestAskPrice";
    public static final String BEST_ASK_Qty="bestAskQty";
    public static final String BEST_BID_PRICE="bestBidPrice";
    public static final String BEST_BID_QTY="bestBidQty";
    public static final String FUTURE_SELL ="futureSell" ;

    public static final String SYMBOL_TICKS_INFO="symbol_ticks_info";
    public static final String EXCHANGE="exchange";
    public static final String BINANCE="binance";
    public static final String SYMBOL ="symbol";
    public static final String FUNDING_RATE="fundingRate";

    public static final String FUTURE_BID_RPICE="futureBidPrice";
    public static final String FUTURE_ASK_PRICE="futureAskPrice";
    public static final String SPOT_BID_PRICE="spotBidPrice";
    public static final String SPOT_ASK_PRICE="spotAskPrice";


    public static final String FUTURE_SELL_OPEN = "FSO";
    public static final String FUTURE_SELL_CLOSE = "FSC";

}
