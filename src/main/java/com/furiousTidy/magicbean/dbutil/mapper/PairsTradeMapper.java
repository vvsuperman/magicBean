package com.furiousTidy.magicbean.dbutil.mapper;

import org.apache.ibatis.jdbc.SQL;

public class PairsTradeMapper {

    public String findPairsTradeBySymbol(){
        SQL sql = new SQL();
        sql.SELECT("id,symbol,openId,openRatio,closeId,closeRatio")
                .FROM("pairs_trade")
                .WHERE("symbol = #{symbol}");
        return sql.toString();
    }

    public String insertPairsTrade(){
        return new SQL()
                .INSERT_INTO("pairs_trade")
                .INTO_COLUMNS("symbol","openId","closeId","openRatio","closeRatio")
                .INTO_VALUES("#{pairsTradeModel.symbol}","#{pairsTradeModel.openId}","#{pairsTradeModel.closeId}","#{pairsTradeModel.openRatio}","#{pairsTradeModel.closeRatio}")
                .toString();
    }
}
