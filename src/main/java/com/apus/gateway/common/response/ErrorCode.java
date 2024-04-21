package com.apus.gateway.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ErrorCode {
  @JsonInclude(JsonInclude.Include.NON_NULL)
  private Integer httpCode;
  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String code;
  private String message;

  public ErrorCode(String code) {
    this.code = code;
  }

  public ErrorCode(Integer httpCode, String code) {
    this.httpCode = httpCode;
    this.code = code;
  }
}
