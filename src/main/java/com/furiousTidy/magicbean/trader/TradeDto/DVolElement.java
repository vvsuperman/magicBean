package com.furiousTidy.magicbean.trader.TradeDto;

import lombok.Data;

@Data
public class DVolElement {
    long date;
    String instrument;
    float open;
    float high;
    float low;
    float close;


}
