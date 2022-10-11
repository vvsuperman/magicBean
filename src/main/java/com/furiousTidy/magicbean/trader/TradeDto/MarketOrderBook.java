package com.furiousTidy.magicbean.trader.TradeDto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.TreeMap;

@Data
public class MarketOrderBook {

    String symbol;

    int depth5;

    TreeMap<BigDecimal, BigDecimal> bidMap =  new TreeMap<BigDecimal, BigDecimal>(new Comparator<BigDecimal>() {
        @Override
        public int compare(BigDecimal o1, BigDecimal o2) {
            return o2.compareTo(o1);
        }
    });

    TreeMap<BigDecimal, BigDecimal> askMap =  new TreeMap<BigDecimal, BigDecimal>(new Comparator<BigDecimal>() {
        @Override
        public int compare(BigDecimal o1, BigDecimal o2) {
            return o1.compareTo(o2);
        }
    });

}
