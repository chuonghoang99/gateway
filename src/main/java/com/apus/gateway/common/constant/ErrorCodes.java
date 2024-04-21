package com.apus.gateway.common.constant;

import com.apus.gateway.common.response.ErrorCode;

public final class ErrorCodes {
  public static final ErrorCode SOMETHING_WRONG = new ErrorCode("error.somethingWrong");

  public static final ErrorCode INVALID_GATEWAY_CLIENT = new ErrorCode("error.invalidGatewayClient");
  public static final ErrorCode USER_DOES_NOT_EXISTS = new ErrorCode("error.userDoesNotExists");
  public static final ErrorCode USER_IS_NOT_ACTIVE = new ErrorCode("error.userIsNotActive");
  public static final ErrorCode INVALID_PASSWORD = new ErrorCode("error.invalidPassword");
  public static final ErrorCode INIT_SESSION_FAILED = new ErrorCode("error.initSessionFailed");
  public static final ErrorCode REMOVE_SESSION_FAILED = new ErrorCode("error.removeSessionFailed");
  public static final ErrorCode INVALID_REFRESH_TOKEN = new ErrorCode("error.invalidRefreshToken");
  public static final ErrorCode INVALID_OTP = new ErrorCode("error.invalidOtp");

  public static final ErrorCode INVALID_SCOPES = new ErrorCode("error.invalidScopes");
  public static final ErrorCode INVALID_CLIENT = new ErrorCode("error.invalidClient");
  public static final ErrorCode INACTIVE_CLIENT = new ErrorCode("error.inactiveClient");
  public static final ErrorCode CLIENT_DOES_NOT_EXISTS = new ErrorCode("error.clientDoesNotExists");

  private ErrorCodes() {

  }
}
