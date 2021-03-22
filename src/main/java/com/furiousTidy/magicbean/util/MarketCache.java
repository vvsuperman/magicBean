package com.furiousTidy.magicbean.util;


//存储市场信息


import com.binance.api.client.domain.account.AssetBalance;
import com.binance.api.client.domain.event.OrderTradeUpdateEvent;
import com.binance.api.client.domain.general.SymbolInfo;
import com.binance.client.model.event.MarkPriceEvent;
import com.binance.client.model.market.ExchangeInfoEntry;
import com.binance.client.model.market.MarkPrice;
import com.binance.client.model.trade.AccountInformation;
import com.binance.client.model.trade.Asset;
import com.binance.client.model.user.BalanceUpdate;
import com.binance.client.model.user.OrderUpdate;
import com.binance.client.model.user.UserDataUpdateEvent;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;

@Service
public class MarketCache {
    //lock
    public static Map<String, Lock> eventLockCache = new HashMap<>();

    //lock
    public static Map<String, AtomicBoolean> closeLockCache = new HashMap<>();


    //symbol-futurerate cache store symbol and futrerate
    public static Map<String, BigDecimal> futureRateCache = new HashMap<>();
    //futurerate-symbol cache store the futurerate and symbol
    public static TreeMap<BigDecimal, String> fRateSymbolCache = new TreeMap<BigDecimal, String>(
            (Comparator<BigDecimal>) (a, b) -> b.compareTo(a)
    );
    //step size cache
    public static Map<String, Integer[]> stepSizeCache = new HashMap<>();
    //期货信息,exchangeinfo
    public static Map<String,ExchangeInfoEntry> futureInfoCache = new HashMap<>();
    //现货信息
    public static Map<String, SymbolInfo> spotInfoCache =new HashMap<>();


    //合约最佳挂单
    public static Map<String,HashMap<String,BigDecimal>> futureTickerMap = new HashMap<String, HashMap<String, BigDecimal>>();

    //实时资金费率
    public static List<MarkPrice> markPriceList = new LinkedList<>();

    // 合约balance信息
    public static TreeMap<String, BalanceUpdate> futureBalanceCache = new TreeMap();

    // 合约position信息
    public static TreeMap futurePositionCache = new TreeMap();

    // 合约订单信息
    public static HashMap<Long,OrderUpdate> futureOrderCache = new HashMap();


    //现货最佳挂单
    public static Map<String,HashMap<String,BigDecimal>> spotTickerMap = new HashMap<String, HashMap<String, BigDecimal>>();


    // 现货用户持仓信息
    public static HashMap<String , AssetBalance> spotBalanceCache = new HashMap();

    // 现货用户订单信息
    public static HashMap<Long,OrderTradeUpdateEvent> spotOrderCache = new HashMap();


}
