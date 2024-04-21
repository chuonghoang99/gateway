package com.apus.gateway.api;

import com.apus.gateway.api.dto.*;
import com.apus.gateway.common.response.BaseResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@Validated
public interface OAuthApi {

  @PostMapping("/login")
  BaseResponse<LoginResponse> login(@RequestBody @Valid LoginRequest request);

  @PostMapping("/otp-login")
  BaseResponse<LoginResponse> otpLogin(@RequestBody @Valid OtpLoginRequest request);

  @PostMapping("/refresh-token")
  BaseResponse<LoginResponse> refreshToken(@RequestBody @Valid RefreshTokenRequest request);

  @PostMapping("/client-login")
  BaseResponse<ClientLoginResponse> clientLogin(@RequestBody @Valid ClientLoginRequest request);

  @PostMapping("/logout")
  void logout(@RequestParam @Schema(description = "The jti of refresh-token.") String jti);

  @DeleteMapping("/evict-role-cache")
  void evictRoleCache();
}
