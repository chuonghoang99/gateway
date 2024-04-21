package com.apus.gateway.api.dto;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;

@Getter
@Setter
public class OtpLoginRequest {
  @NotBlank
  private String otp;
  private String deviceInfo;
}
