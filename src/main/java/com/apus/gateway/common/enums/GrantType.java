package com.apus.gateway.common.enums;

public enum GrantType {
  PASSWORD("password"),
  REFRESH_TOKEN("refresh_token");

  private String value;

  GrantType(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }
}
