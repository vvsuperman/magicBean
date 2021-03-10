package com.furiousTidy.magicbean.util;


//存储市场信息


import com.binance.api.client.domain.event.OrderTradeUpdateEvent;
import com.binance.api.client.domain.general.SymbolInfo;
import com.binance.client.model.event.MarkPriceEvent;
import com.binance.client.model.market.ExchangeInfoEntry;
import com.binance.client.model.market.MarkPrice;
import com.binance.client.model.trade.AccountInformation;
import com.binance.client.model.trade.Asset;
import com.binance.client.model.user.OrderUpdate;
import com.binance.client.model.user.UserDataUpdateEvent;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

@Service
public class MarketCache {
    //market price cache for future
    public static Map<String, MarkPriceEvent> markPriceEventMap = new HashMap<>();
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
    public static TreeMap futureBalanceCache = new TreeMap();

    // 合约position信息
    public static TreeMap futurePositionCache = new TreeMap();

    // 合约订单信息
    public static HashMap<Long,OrderUpdate> futureOrderCache = new HashMap();


    //现货最佳挂单
    public static Map<String,HashMap<String,BigDecimal>> spotTickerMap = new HashMap<String, HashMap<String, BigDecimal>>();


    // 现货用户持仓信息
    public static HashMap spotBalanceCache = new HashMap();

    // 现货用户订单信息
    public static HashMap<Long,OrderTradeUpdateEvent> spotOrderCache = new HashMap();


}
