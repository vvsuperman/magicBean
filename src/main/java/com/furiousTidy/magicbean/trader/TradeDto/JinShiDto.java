package com.furiousTidy.magicbean.trader.TradeDto;

import lombok.Data;

//金十数据的快讯数据
@Data
public class JinShiDto {
    String id;
    String country;
    String actual;
    String consensus;
    String unit;
    String revised;
    String name;
    String pub_time;
    String indicator_id;
    String time_period;
}
