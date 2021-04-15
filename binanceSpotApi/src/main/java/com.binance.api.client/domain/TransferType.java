package com.binance.api.client.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Status of a submitted order.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public enum TransferType {
  SPOT_TO_MARGIN("1"), MARGIN_TO_SPOT("2"),MAIN_UMFUTURE("MAIN_UMFUTURE"),UMFUTURE_MAIN("UMFUTURE_MAIN");


  private String value;

  TransferType(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }
}
