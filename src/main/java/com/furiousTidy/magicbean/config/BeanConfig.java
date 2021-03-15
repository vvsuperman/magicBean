package com.furiousTidy.magicbean.config;

import java.math.BigDecimal;

public class BeanConfig {

    public static final BigDecimal openPriceGap =new BigDecimal("0.005"); //交易条件，千分之五
    public static final BigDecimal closePriceGap = new BigDecimal("0.003");  //平仓条件千分之三
    public static final BigDecimal standardTradeUnit=new BigDecimal("15"); //标准交易单元，默认为100usdt
    public static final BigDecimal MIN_OPEN_UNIT = new BigDecimal(10);

    public static final long orderExpireTime =5000;// 订单失效时间: 20s

    public static final int priorNum =5;

    //test future api
    public static final String FUTURE_API_KEY = "00f8530794fa9da45c5dde274ad8c5121d495d10e53853a089e482e0dcebba68";
    public static final String FUTURE_SECRET_KEY = "a11b4277df6bd76ba52363acc509ffb2f8b346ee141aac8efe1ad0d1c1c4c83b";

    //现货测试
    public static final String SPOT_API_KEY = "AI349hcSE7HLymNLXTrpNfeCFptoqrhpAzblO0DPiRPj67Kq7QLG9AfEHBaObpH8";
    public static final String SPOT_SECRET_KEY = "LmPr3JxEeNbbcSEtJ6KCtXKCMXBmLc0LWDGKvEXdlk7xDhs4nFnWzOYTqcPNLfIk";

    //最小下单单元

    //prd mengna
//  public static final String API_KEY = "4bXoFQbvoe18Xy2B7dWJxqfRTU78DPduiBDHScIHQ0aFXI6tNYeVEIsdNwfTjmX9";
//  public static final String SECRET_KEY = "Io5imcwwWzKyW1oWsJ9LjjIY9Dk2fkpqtjuZwrdRYzDON8UEDP5sjPPDMIhKC9KK";


//    public static final String API_KEY = "ypDfS0pu16G7MrY1LD7PFXeIqNoWUI84l19XDrT2WCq4vQKLtHUfkgiQ3nFO8kX5";
//    public static final String SECRET_KEY = "MWPNHKLB9nMD4L9V7Q1WevicWmswVqMJmPS5hbBwf0XzbxdJNzTFNcEqtEcUJ2kc";


    public static void main(String[] args){
        System.out.println(new BigDecimal("-0.001").subtract(closePriceGap));
    }
}
