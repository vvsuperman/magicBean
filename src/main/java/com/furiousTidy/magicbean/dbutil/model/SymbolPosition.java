package com.furiousTidy.magicbean.dbutil.model;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SymbolPosition {

    String symbol;
    BigDecimal futureQty = BigDecimal.ZERO;
    BigDecimal spotQty = BigDecimal.ZERO;
}
