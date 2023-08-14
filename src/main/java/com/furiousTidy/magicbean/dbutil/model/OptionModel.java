package com.furiousTidy.magicbean.dbutil.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Date;

@Data
public class OptionModel {
    String symbol;
    String instrumentName;
    String tradeId;
    float price;       //期权成交价
    float markPrice;   //期权市价
    float indexPrice;  //标的价格
    float iv;
    float amount;
    String direction;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",locale = "zh",timezone = "GMT+8")
    Date tradeTime;
    int tickDirection;  //不晓得是什么，先存着

}
