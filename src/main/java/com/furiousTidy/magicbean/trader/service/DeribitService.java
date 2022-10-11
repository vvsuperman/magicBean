package com.furiousTidy.magicbean.trader.service;


//get deribit option chain info
public class DeribitService {

    //https://history.deribit.com/api/v2/public/get_last_trades_by_currency_and_time?currency=ETH&end_timestamp=1630490400000&count=1000&include_old=true
    static String url = "https://history.deribit.com/api/v2/public/get_last_trades_by_currency_and_time?currency=ETH" +
            "&count=1000&include_old=true&start_timestamp=";

    static String dateTime="2020-01-01";

    public void getOptionInfo(){
//        1 按照小时循环
//        2 获取1小时所有的instruments,筛选出期权,算出初次价格的bidprice,和askprice，
//        3 存入influxdb

    }
}
