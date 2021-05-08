package com.furiousTidy.magicbean.util;

import com.binance.api.client.constant.BinanceApiConstants;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.math.BigDecimal;

@Data
public class BookTickerModel {

    private long updateId;

    private String symbol;

    private long tradeTime;

    private long spotTickDelayTime;

    private BigDecimal bidPrice;

    private BigDecimal bidQuantity;

    private BigDecimal askPrice;

    private BigDecimal askQuantity;


}