package com.furiousTidy.magicbean.dbutil.model;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PairsTradeModel {
    String id;
    String symbol;
    String openId;
    String closeId;
    BigDecimal openRatio;
    BigDecimal closeRatio;
}
