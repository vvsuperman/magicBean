package com.furiousTidy.magicbean.dbutil.model;

import lombok.Data;

@Data
public class OrderModel {
    String symbol;
    String clientOrderId;
    String type;
}
