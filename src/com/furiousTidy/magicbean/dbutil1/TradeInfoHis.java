package com.furiousTidy.magicbean.dbutil1;

public class TradeInfoHis {
    private Integer id;

    private String symbol;

    private Double futureprice;

    private Double futureqty;

    private String createtime;

    private Double spotprice;

    private Double spotqty;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol == null ? null : symbol.trim();
    }

    public Double getFutureprice() {
        return futureprice;
    }

    public void setFutureprice(Double futureprice) {
        this.futureprice = futureprice;
    }

    public Double getFutureqty() {
        return futureqty;
    }

    public void setFutureqty(Double futureqty) {
        this.futureqty = futureqty;
    }

    public String getCreatetime() {
        return createtime;
    }

    public void setCreatetime(String createtime) {
        this.createtime = createtime == null ? null : createtime.trim();
    }

    public Double getSpotprice() {
        return spotprice;
    }

    public void setSpotprice(Double spotprice) {
        this.spotprice = spotprice;
    }

    public Double getSpotqty() {
        return spotqty;
    }

    public void setSpotqty(Double spotqty) {
        this.spotqty = spotqty;
    }
}