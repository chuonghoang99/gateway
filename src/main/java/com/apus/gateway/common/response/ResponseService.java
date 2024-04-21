package com.apus.gateway.common.response;

public interface ResponseService {

  <T> BaseResponse<T> success(T data);

  BaseResponse<Object> error(ErrorCode errorCode);

  String getTraceId();
}
