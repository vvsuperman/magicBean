package com.furiousTidy.magicbean.dbutil;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class TradeInfoModel {
    String symbol;
    String orderId;
    BigDecimal futurePrice;
    BigDecimal futureQty;
    BigDecimal spotPrice;
    BigDecimal spotQty;
    String createTime;
}
