package com.furiousTidy.magicbean.trader;


import com.binance.api.client.BinanceApiClientFactory;
import com.binance.api.client.BinanceApiRestClient;
import com.binance.client.RequestOptions;
import com.binance.client.SyncRequestClient;
import com.binance.client.model.market.SymbolOrderBook;
import com.furiousTidy.magicbean.constant.BeanConfig;

import java.math.BigDecimal;

//计算下持仓情况
public class BeanCalcuate {

    //合约客户端
    private static SyncRequestClient syncRequestClient = SyncRequestClient.create(BeanConfig.API_KEY, BeanConfig.SECRET_KEY,
            new RequestOptions());

    //现货客户端
    private static BinanceApiClientFactory factory = BinanceApiClientFactory.newInstance(BeanConfig.API_KEY, BeanConfig.SECRET_KEY);
    private static BinanceApiRestClient client = factory.newRestClient();


    public void calculateSymbol(String symbol, BigDecimal spotQuantity, BigDecimal futureQuantity){


        //获取合约最佳价格
        SymbolOrderBook futureOrder=syncRequestClient.getSymbolOrderBookTicker(symbol).get(0);


        //获取现货持仓


        //获取现货价格


        //计算目前盈亏

    }
}
