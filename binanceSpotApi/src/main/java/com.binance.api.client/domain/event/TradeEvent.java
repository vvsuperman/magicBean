package com.binance.api.client.domain.event;

import com.binance.api.client.constant.BinanceApiConstants;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.math.BigDecimal;

/**
 *
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class TradeEvent {

    @JsonProperty("e")
    private String eventType;

    @JsonProperty("E")
    private long eventTime;

    @JsonProperty("T")
    private long tradeTime;

    @JsonProperty("s")
    private String symbol;

    @JsonProperty("p")
    private BigDecimal price;

    @JsonProperty("q")
    private BigDecimal qty;

    public long getTradeTime(){return tradeTime;}

    public void setTradeTime(long tradeTime){this.tradeTime = tradeTime;}

    public BigDecimal getPrice(){return price;}

    public void setPrice(BigDecimal price){this.price = price;}

    public BigDecimal getQty(){return qty;}

    public void setQty(BigDecimal qty){ this.qty = qty;}

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public long getEventTime() {
        return eventTime;
    }

    public void setEventTime(long eventTime) {
        this.eventTime = eventTime;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, BinanceApiConstants.TO_STRING_BUILDER_STYLE)
                .append("eventType", eventType)
                .append("eventTime", eventTime)
                .append("symbol", symbol)
                .append("tradeTime",tradeTime)
                .append("price",price)
                .append("qty",qty)
                .toString();
    }
}
