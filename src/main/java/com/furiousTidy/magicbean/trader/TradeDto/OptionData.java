package com.furiousTidy.magicbean.trader.TradeDto;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode
public class OptionData {
    @ExcelProperty("date")
    private String date;
    @ExcelProperty("instrument_name")
    private String instrumentName;
    @ExcelProperty("iv")
    private String iv;
    @ExcelProperty("mark_price")
    private String markPrice;
    @ExcelProperty("index_price")
    private String indexPrice;


}
