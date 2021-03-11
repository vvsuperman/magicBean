package com.furiousTidy.magicbean.dbutil;

import lombok.Data;

@Data
public class PairsTradeModel {
    String symbol;
    String openBidPrice;
    String openBidQty;
    String openAskPrice;
    String openAskQty;
    String closeBidPrice;
    String closeBidQty;
    String closeAskPrice;
    String closeAskQty;
    int status;
    String type;
}
