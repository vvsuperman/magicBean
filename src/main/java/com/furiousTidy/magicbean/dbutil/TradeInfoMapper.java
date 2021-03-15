package com.furiousTidy.magicbean.dbutil;

import org.apache.ibatis.jdbc.SQL;

public class TradeInfoMapper {

    public String findSymbolOrderList(){
        SQL sql = new SQL();
        sql.SELECT("symbol,futurePrice,futureQty")
           .FROM("pairs_trade_info")
           .WHERE("symbol = #{symbolModel.symbol}")
           .WHERE("status = #{symbolModel.status}")
           .WHERE("type=#{symbolModel.type}");
        return sql.toString();
    }

    public String insertPairsTrade(){
        return new SQL()
                .INSERT_INTO("pairs_trade_info")
                .INTO_COLUMNS("symbol","orderId","type","status")
                .INTO_VALUES("#{pairsTradeModel.symbol}","#{pairsTradeModel.orderId}","#{pairsTradeModel.type}","#{pairsTradeModel.status}")
                .INTO_COLUMNS("futurePrice","futureQty","spotPrice","spotQty","createTime")
                .INTO_VALUES("#{pairsTradeModel.futurePrice}","#{pairsTradeModel.futureQty}","#{pairsTradeModel.spotPrice}","#{pairsTradeModel.spotQty}","#{pairsTradeModel.createTime}")
                .INTO_COLUMNS("closeBidPrice","closeBidQty","closeAskPrice","closeAskQty","closeTime")
                .INTO_VALUES("#{pairsTradeModel.closeBidPrice}","#{pairsTradeModel.closeBidQty}","#{pairsTradeModel.closeAskPrice}","#{pairsTradeModel.closeAskQty}","#{pairsTradeModel.closeTime}")
                .INTO_COLUMNS("openRatio","closeRatio")
                .INTO_VALUES("#{pairsTradeModel.openRatio}","#{pairsTradeModel.closeRatio}")
                .toString();
    }


}
