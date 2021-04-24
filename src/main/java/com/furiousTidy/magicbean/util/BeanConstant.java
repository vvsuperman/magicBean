package com.furiousTidy.magicbean.util;

import com.furiousTidy.magicbean.dbutil.model.PairsTradeModel;
import com.furiousTidy.magicbean.dbutil.model.TradeInfoModel;
import sun.rmi.server.InactiveGroupException;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class BeanConstant {


    public static Map<String, TradeInfoModel> tradeInfoMap = new HashMap<>();
    public static List<PairsTradeModel> pairsTradeList = new ArrayList<>();



    public static BigDecimal b4 = new BigDecimal("0.004");
    public static BigDecimal b5 = new BigDecimal("0.005");
    public static BigDecimal b6 = new BigDecimal("0.006");
    public static BigDecimal b7 = new BigDecimal("0.007");
    public static BigDecimal b8 = new BigDecimal("0.008");
    public static BigDecimal b9 = new BigDecimal("0.009");
    public static BigDecimal b10 = new BigDecimal("0.01");

    public static int BigThanB4 = 0;
    public static int BigThanB5 = 0;
    public static int BigThanB6 = 0;
    public static int BigThanB7 = 0;
    public static int BigThanB8 = 0;
    public static int BigThanB9 = 0;
    public static int BigThanB10 = 0;



    public static Set<String> openImpactSet = new HashSet<>();

    public static Set<String> closeImpactSet = new HashSet<>();

    public static Set<String> closeProcessingSet = new HashSet<>();



    public static boolean NETWORK_DELAYED = false;
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
