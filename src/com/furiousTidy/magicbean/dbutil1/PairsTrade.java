package com.furiousTidy.magicbean.dbutil1;

public class PairsTrade {
    private Integer id;

    private String symbol;

    private String openid;

    private String closeid;

    private Double openratio;

    private Double closeratio;

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

    public String getOpenid() {
        return openid;
    }

    public void setOpenid(String openid) {
        this.openid = openid == null ? null : openid.trim();
    }

    public String getCloseid() {
        return closeid;
    }

    public void setCloseid(String closeid) {
        this.closeid = closeid == null ? null : closeid.trim();
    }

    public Double getOpenratio() {
        return openratio;
    }

    public void setOpenratio(Double openratio) {
        this.openratio = openratio;
    }

    public Double getCloseratio() {
        return closeratio;
    }

    public void setCloseratio(Double closeratio) {
        this.closeratio = closeratio;
    }
}