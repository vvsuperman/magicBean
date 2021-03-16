package com.furiousTidy.magicbean.dbutil.mapper;

import org.apache.ibatis.jdbc.SQL;

public class TradeInfoMapper {

    public String getTradeInfoById(){
        SQL sql = new SQL();
        sql.SELECT("symbol,futurePrice,futureQty,spotPrice,spotQty,createTime")
           .FROM("trade_info")
           .WHERE("orderId = #{orderId}");

        return sql.toString();
    }

    public String insertTradeInfo(){
        return new SQL()
                .INSERT_INTO("trade_info")
                .INTO_COLUMNS("symbol","orderId")
                .INTO_VALUES("#{tradeInfoModel.symbol}","#{tradeInfoModel.orderId}")
                .INTO_COLUMNS("futurePrice","futureQty","spotPrice","spotQty","createTime")
                .INTO_VALUES("#{tradeInfoModel.futurePrice}","#{tradeInfoModel.futureQty}","#{tradeInfoModel.spotPrice}","#{tradeInfoModel.spotQty}","#{tradeInfoModel.createTime}")
                .toString();
    }


}
