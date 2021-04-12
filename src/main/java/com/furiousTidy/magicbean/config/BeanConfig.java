package com.furiousTidy.magicbean.config;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class BeanConfig {
    //network is poor,sleep 10 min
    public static  long NET_DELAY_TIME = 6000000;
    public static  BigDecimal GAP_FACTOR = new BigDecimal("0.0003");
    public static  String TRADE_ALWAYS_OPEN = "false";
    public static  String TRADE_ALWAYS_CLOSE = "false";

    public static  String FUND_RATE_OPEN_THRESHOLD = "0.0012";
    public static  String FUND_RATE_CLOSE_THRESHOLD = "0.001";
    public static  BigDecimal OPEN_PRICE_GAP =new BigDecimal("0.004"); //交易条件，千分之五
    public static  BigDecimal CLOSE_PRICE_GAP = new BigDecimal("0.0023");  //平仓条件千分之2.3，不亏就行
    public static  BigDecimal STANDARD_TRADE_UNIT =new BigDecimal("15"); //标准交易单元
    public static  BigDecimal MIN_OPEN_UNIT = new BigDecimal(10);
    public static  BigDecimal ENOUTH_MOENY_UNIT =new BigDecimal("60"); //多线程并发太快，留60刀


    public static  long ORDER_EXPIRE_TIME =3;// 订单失效时间

    public static  int PRIOR_NUM =10;  //资金费率排名前10的

    public static  Long SLEEP_TIME = new Long("1000");

    //test future api
//    public static  String FUTURE_API_KEY = "00f8530794fa9da45c5dde274ad8c5121d495d10e53853a089e482e0dcebba68";
//    public static  String FUTURE_SECRET_KEY = "a11b4277df6bd76ba52363acc509ffb2f8b346ee141aac8efe1ad0d1c1c4c83b";

    //现货测试 mengna
//    public static  String SPOT_API_KEY = "AI349hcSE7HLymNLXTrpNfeCFptoqrhpAzblO0DPiRPj67Kq7QLG9AfEHBaObpH8";
//    public static  String SPOT_SECRET_KEY = "LmPr3JxEeNbbcSEtJ6KCtXKCMXBmLc0LWDGKvEXdlk7xDhs4nFnWzOYTqcPNLfIk";

    //现货测试 mengna-new
//    public static  String SPOT_API_KEY = "8SU39DpTOwCozHKYZOeVBVmhhqDRqSFH4oBtF7Es0P7gFUD7dHKVa3s0DlOylRAj";
//    public static  String SPOT_SECRET_KEY = "Qlnxc4rej7KIr7cPQ1Bf33eYwJAya3NrSpYM5LhcH9gRu1fc0aVOGLnnJKWe8wmM";

    //最小下单单元

    //prd mengna
//  public static final String API_KEY = "4bXoFQbvoe18Xy2B7dWJxqfRTU78DPduiBDHScIHQ0aFXI6tNYeVEIsdNwfTjmX9";
//  public static final String SECRET_KEY = "Io5imcwwWzKyW1oWsJ9LjjIY9Dk2fkpqtjuZwrdRYzDON8UEDP5sjPPDMIhKC9KK";

    //prd laoma
    public static final String API_KEY = "ypDfS0pu16G7MrY1LD7PFXeIqNoWUI84l19XDrT2WCq4vQKLtHUfkgiQ3nFO8kX5";
    public static final String SECRET_KEY = "MWPNHKLB9nMD4L9V7Q1WevicWmswVqMJmPS5hbBwf0XzbxdJNzTFNcEqtEcUJ2kc";


    public static void main(String[] args){
        System.out.println(new BigDecimal("-0.001").subtract(CLOSE_PRICE_GAP));
    }
}
