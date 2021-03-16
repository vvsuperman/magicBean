package com.furiousTidy.magicbean.dbutil;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PairsTradeModel {
    String symbol;
    String openId;
    String closeId;
    BigDecimal openRatio;
    BigDecimal closeRatio;
}
