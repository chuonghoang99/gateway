package com.apus.gateway.api;

import com.apus.gateway.api.dto.*;
import com.apus.gateway.common.response.BaseResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/oauth")
@RequiredArgsConstructor
public class OAuthController implements OAuthApi {
  private final OAuthApi service;

  @Override
  public BaseResponse<LoginResponse> login(LoginRequest request) {
    return service.login(request);
  }

  @Override
  public BaseResponse<LoginResponse> otpLogin(OtpLoginRequest request) {
    return service.otpLogin(request);
  }

  @Override
  public BaseResponse<LoginResponse> refreshToken(RefreshTokenRequest request) {
    return service.refreshToken(request);
  }

  @Override
  public BaseResponse<ClientLoginResponse> clientLogin(ClientLoginRequest request) {
    return service.clientLogin(request);
  }

  @Override
  public void logout(String jti) {
    service.logout(jti);
  }

  @Override
  public void evictRoleCache() {
    service.evictRoleCache();
  }
}
