package com.furiousTidy.magicbean.dbutil;

import org.apache.ibatis.jdbc.SQL;

public class PairsTradeMapper {

    public String findSymbolOrderList(){
        SQL sql = new SQL();
        sql.SELECT("symbol,openBidPrice,openBidQty")
           .FROM("pairs_trade_info")
           .WHERE("symbol = #{symbolModel.symbol}")
           .WHERE("status = #{symbolModel.status}")
           .WHERE("type=#{symbolModel.type}");
        return sql.toString();
    }


}
