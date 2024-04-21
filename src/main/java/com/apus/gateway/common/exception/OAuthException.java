package com.apus.gateway.common.exception;

import com.apus.gateway.common.response.ErrorCode;
import lombok.Getter;

@Getter
public class OAuthException extends RuntimeException {
  private final transient ErrorCode errorCode;

  public OAuthException(String msg, ErrorCode errorCode) {
    super(msg);
    this.errorCode = errorCode;
  }

  public OAuthException(ErrorCode errorCode) {
    this.errorCode = errorCode;
  }
}
