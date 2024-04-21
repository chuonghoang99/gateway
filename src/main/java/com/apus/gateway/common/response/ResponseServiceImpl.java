package com.apus.gateway.common.response;

import brave.Span;
import brave.Tracer;
import com.apus.gateway.common.constant.Messages;
import com.apus.gateway.common.translate.TranslateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ResponseServiceImpl implements ResponseService {
  private final TranslateService translateService;
  private final Tracer tracer;

  @Override
  public <T> BaseResponse<T> success(T data) {
    return BaseResponse.<T>builder()
        .message(translateService.translate(Messages.SUCCESS))
        .traceId(getTraceId())
        .data(data)
        .build();
  }

  @Override
  public BaseResponse<Object> error(ErrorCode errorCode) {
    errorCode.setMessage(translateService.translate(errorCode.getCode()));
    return BaseResponse.builder()
        .message(translateService.translate(Messages.ERROR))
        .traceId(getTraceId())
        .errorCodes(new ErrorCode[] {errorCode})
        .build();
  }

  @Override
  public String getTraceId() {
    if (tracer == null) {
      return null;
    }
    Span span = tracer.currentSpan();
    if (span == null) {
      return null;
    }
    return span.context().traceIdString();
  }
}
