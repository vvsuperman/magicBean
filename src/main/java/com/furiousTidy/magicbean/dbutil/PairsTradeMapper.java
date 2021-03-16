package com.furiousTidy.magicbean.dbutil;

import org.apache.ibatis.jdbc.SQL;

public class PairsTradeMapper {

    public String findPairsTradeBySymbol(){
        SQL sql = new SQL();
        sql.SELECT("symbol,openId,openRatio,closeId,closeRatio")
                .FROM("pairs_trade")
                .WHERE("symbol = #{symbol}");
        return sql.toString();
    }
}
