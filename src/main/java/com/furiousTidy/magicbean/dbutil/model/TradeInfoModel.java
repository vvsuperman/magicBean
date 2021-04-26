package com.furiousTidy.magicbean.dbutil.model;

import com.furiousTidy.magicbean.util.BeanConstant;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class TradeInfoModel {
    String symbol;
    String orderId;
    BigDecimal futurePrice;
    BigDecimal futureQty;
    BigDecimal spotPrice;
    BigDecimal spotQty;
    String createTime;
    String updateTime;
}
