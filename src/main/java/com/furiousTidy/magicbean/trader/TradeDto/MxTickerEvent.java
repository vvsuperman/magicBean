package com.furiousTidy.magicbean.trader.TradeDto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;


@Data
public class MxTickerEvent {
    String symbol;

    private BigDecimal price;

    private BigDecimal maxBidPrice;

    private BigDecimal minAskPrice;

    private long tradeTime;
}
