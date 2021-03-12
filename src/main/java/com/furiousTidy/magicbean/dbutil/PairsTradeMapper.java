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

    public String insertPairsTrade(){
        return new SQL()
                .INSERT_INTO("pairs_trade_info")
                .INTO_COLUMNS("symbol", "openBidPrice","openBidQty","openAskPrice","openAskQty","status")
                .INTO_VALUES("#{pairsTradeModel.symbol}","#{pairsTradeModel.openBidPrice}","#{pairsTradeModel.openBidQty}","#{pairsTradeModel.openAskPrice}","#{pairsTradeModel.openAskQty}", "#{pairsTradeModel.status}")
                .INTO_COLUMNS("closeBidPrice","closeBidQty","closeAskPrice","closeAskQty")
                .INTO_VALUES("#{pairsTradeModel.closeBidPrice}","#{pairsTradeModel.closeBidQty}","#{pairsTradeModel.closeAskPrice}","#{pairsTradeModel.closeAskQty}")
                .toString();
    }


}
