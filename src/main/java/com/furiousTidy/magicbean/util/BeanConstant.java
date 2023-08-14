package com.furiousTidy.magicbean.util;

import com.furiousTidy.magicbean.dbutil.model.PairsTradeModel;
import com.furiousTidy.magicbean.dbutil.model.TradeInfoModel;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class BeanConstant {


    public static List<String> MONTH_LIST =Arrays.asList("JAN","FEB","MAR","APR","MAY","JUN","JUL","AUG","SEP","OCT","NOV","DEC");

    public static long NANO_2_MS= 1000000;

    public static String FED = "美联储利率决定";
    public static float FED_THRESHOLD = 0.01f;

    public static String CPI = "季调CPI年率";
    public static float CPI_THRESHOLD = 0.01f;

    public static String JINSHIURL = "https://cdn-rili.jin10.com/data/";
    public static String JINSHIURL_SUB1 = "/economics.json?_=";
    public static String JINSHIURL_SUB2 = "&date=";
    public static String JINSHIURL_SUB3 = "0";


    public static AtomicBoolean TradeProcess = new AtomicBoolean(false);

    public static boolean GAP_2_BIG = false;
    public static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");


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
