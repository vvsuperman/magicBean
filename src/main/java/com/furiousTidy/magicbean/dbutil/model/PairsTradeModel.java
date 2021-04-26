package com.furiousTidy.magicbean.dbutil.model;

import com.furiousTidy.magicbean.trader.TradeUtil;
import com.furiousTidy.magicbean.util.BeanConstant;
import lombok.Data;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;

@Data
public class PairsTradeModel {
    String id;
    String symbol;
    String openId;
    String closeId;
    BigDecimal openRatio;
    BigDecimal closeRatio;
    BigDecimal profit;
    String createTime;
    String updateTime;

}
