package com.furiousTidy.magicbean.util;


//存储市场信息


import com.binance.client.model.event.MarkPriceEvent;
import com.binance.client.model.trade.AccountInformation;
import com.binance.client.model.trade.Asset;
import com.binance.client.model.user.UserDataUpdateEvent;

import java.math.BigDecimal;
import java.util.*;

public class MarketCache {

    //合约最佳挂单
    public static Map<String,HashMap<String,BigDecimal>> tickerMap = new HashMap<String, HashMap<String, BigDecimal>>();

    //实时资金费率
    public static List<MarkPriceEvent> markPriceList = new LinkedList<>();

    // 合约balance信息
    public static TreeMap futureBalanceCache = new TreeMap();

    // 合约position信息
    public static TreeMap futurePositionCache = new TreeMap();

    // 合约订单信息
    public static HashMap futureOrderCache = new HashMap();


    // 现货用户持仓信息
    public static HashMap spotBalanceCache = new HashMap();

    // 现货用户订单信息
    public static HashMap spotOrderCache = new HashMap();


}
