package com.apus.gateway.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BaseResponse<T> {
  private String message;
  private String traceId;
  @JsonInclude(JsonInclude.Include.NON_NULL)
  private ErrorCode[] errorCodes;
  @JsonInclude(JsonInclude.Include.NON_NULL)
  private T data;
}