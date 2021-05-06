package com.binance.client.model.event;

import com.binance.client.constant.BinanceApiConstants;
import lombok.Data;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.math.BigDecimal;


@Data
public class SymbolBookTickerEvent {

    private Long orderBookUpdateId;

    private String symbol;

    private BigDecimal bestBidPrice;

    private BigDecimal bestBidQty;

    private BigDecimal bestAskPrice;

    private BigDecimal bestAskQty;

    private long futureTickDelayTime;

    private long eventTime;

    private long tradeTime;

    private long localTime;

}
