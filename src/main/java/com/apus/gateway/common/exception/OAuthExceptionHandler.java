package com.apus.gateway.common.exception;

import com.apus.gateway.common.response.BaseResponse;
import com.apus.gateway.common.response.ResponseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class OAuthExceptionHandler {
  private final ResponseService responseService;

  @ExceptionHandler({OAuthException.class})
  public ResponseEntity<BaseResponse<Object>> handling(OAuthException exception) {
    log.error("OAuthException -> ", exception);
    return ResponseEntity.ok(responseService.error(exception.getErrorCode()));
  }
}
