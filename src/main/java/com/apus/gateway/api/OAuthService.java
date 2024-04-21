package com.apus.gateway.api;

import com.apus.gateway.api.dto.*;
import com.apus.gateway.common.component.RoleCache;
import com.apus.gateway.common.constant.ErrorCodes;
import com.apus.gateway.common.constant.IMaps;
import com.apus.gateway.common.constant.OAuth2Params;
import com.apus.gateway.common.constant.OAuth2Errors;
import com.apus.gateway.common.exception.OAuthException;
import com.apus.gateway.common.response.BaseResponse;
import com.apus.gateway.common.response.ResponseService;
import com.apus.gateway.common.security.SecurityUtils;
import com.apus.gateway.common.security.TokenPayload;
import com.apus.gateway.configuration.GatewayProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class OAuthService implements OAuthApi {
  private final GatewayProperties properties;
  private final ResponseService responseService;
  private final RestTemplate restTemplate;
  private final HazelcastInstance hazelcastInstance;
  private final RoleCache roleCache;

  public OAuthService(GatewayProperties properties,
                      ResponseService responseService,
                      @Qualifier("loadBalancedRestTemplate") RestTemplate restTemplate,
                      @Qualifier("myHazelcast") HazelcastInstance hazelcastInstance,
                      RoleCache roleCache) {
    this.properties = properties;
    this.responseService = responseService;
    this.restTemplate = restTemplate;
    this.hazelcastInstance = hazelcastInstance;
    this.roleCache = roleCache;
  }

  @Override
  public BaseResponse<LoginResponse> login(LoginRequest request) {
    MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
    params.set("username", request.getUsername());
    params.set("password", request.getPassword());
    params.set(OAuth2Params.GRANT_TYPE, OAuth2Params.GRANT_PASSWORD);
    params.set(OAuth2Params.SCOPE, getScopes());
    params.set(OAuth2Params.DEVICE_INFO, request.getDeviceInfo());
    // call to UAA
    var res = sendUserTokenGrant(params);
    log.info("Successfully login of user [{}]!", res.getData().getUserId());
    return res;
  }

  @Override
  public BaseResponse<LoginResponse> otpLogin(OtpLoginRequest request) {
    MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
    params.set("otp", request.getOtp());
    params.set(OAuth2Params.GRANT_TYPE, OAuth2Params.GRANT_OTP);
    params.set(OAuth2Params.SCOPE, getScopes());
    params.set(OAuth2Params.DEVICE_INFO, request.getDeviceInfo());
    // call to UAA
    var res = sendUserTokenGrant(params);
    log.info("Successfully login of user [{}]!", res.getData().getUserId());
    return res;
  }

  @Override
  public BaseResponse<LoginResponse> refreshToken(RefreshTokenRequest request) {
    MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
    params.set("refresh_token", request.getRefreshToken());
    params.set(OAuth2Params.GRANT_TYPE, OAuth2Params.GRANT_REFRESH_TOKEN);
    return sendUserTokenGrant(params);
  }

  @Override
  public BaseResponse<ClientLoginResponse> clientLogin(ClientLoginRequest request) {
    MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
    params.set(OAuth2Params.GRANT_TYPE, OAuth2Params.GRANT_CLIENT_CREDENTIALS);
    params.set(OAuth2Params.SCOPE, StringUtils.join(request.getScopes(), " "));
    params.set(OAuth2Params.IS_EXTERNAL_CLIENT, "");
    return sendClientTokenGrant(params, request);
  }

  @Override
  public void logout(String rfJti) {
    TokenPayload accessTokenPayload = SecurityUtils.getTokenPayload();
    if (accessTokenPayload == null) {
      return;
    }
    String jti = accessTokenPayload.getJti();
    // Make the access-token become invalid
    Long exp = accessTokenPayload.getExp();
    long currentTime = System.currentTimeMillis();
    long ttl = (exp * 1000 - currentTime) / 1000;
    if (ttl > 0) {
      IMap<String, Boolean> expiredTokens = hazelcastInstance.getMap(IMaps.EXPIRED_ACCESS_TOKENS);
      expiredTokens.put(jti, true, ttl, TimeUnit.SECONDS);
    }
    // Clear session (make the refresh-token become invalid)
    removeSession(rfJti);
    log.info("Successfully logout of user {}!", accessTokenPayload.getUsername());
  }

  @Override
  public void evictRoleCache() {
    roleCache.clear();
  }

  private void removeSession(String rfJti) {
    try {
      HttpEntity<Void> request = new HttpEntity<>(new HttpHeaders());
      UriComponentsBuilder uriBuilder = UriComponentsBuilder
          .fromHttpUrl(properties.getUaa().getDeleteRefreshTokenUri())
          .queryParam("jti", rfJti);
      restTemplate.exchange(
          uriBuilder.toUriString(),
          HttpMethod.DELETE,
          request,
          Void.class
      );
    } catch (IllegalStateException e) {
      log.error("Couldn't connect to UAA for removing the session.", e);
      throw new OAuthException("Couldn't connect to UAA for removing the session.", ErrorCodes.REMOVE_SESSION_FAILED);
    }
  }

  private BaseResponse<LoginResponse> sendUserTokenGrant(MultiValueMap<String, String> params) {
    try {
      HttpHeaders reqHeaders = new HttpHeaders();
      // client basic-authentication
      SecurityUtils.addClientBasicAuth(reqHeaders, getClientId(), getClientSecret());
      HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, reqHeaders);
      ResponseEntity<LoginResponse> responseEntity = restTemplate.exchange(
          getTokenEndpoint(),
          HttpMethod.POST,
          request,
          LoginResponse.class
      );
      return responseService.success(responseEntity.getBody());

    } catch (HttpClientErrorException e) {
      log.error("Couldn't send user's token-grant to UAA.", e);
      throw mappingUserError(e);
    } catch (ResourceAccessException | IllegalStateException e) {
      log.error("Couldn't connect to UAA for sending user token-grant.", e);
      throw new OAuthException("Can't connect to UAA for sending user's token-grant.", ErrorCodes.SOMETHING_WRONG);
    }
  }

  private BaseResponse<ClientLoginResponse> sendClientTokenGrant(MultiValueMap<String, String> params,
                                                                 ClientLoginRequest clientLoginRequest) {
    try {
      HttpHeaders reqHeaders = new HttpHeaders();
      // client basic-authentication
      SecurityUtils.addClientBasicAuth(
          reqHeaders,
          clientLoginRequest.getClientId(),
          clientLoginRequest.getClientSecret()
      );
      HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, reqHeaders);
      ResponseEntity<ClientLoginResponse> responseEntity = restTemplate.exchange(
          getTokenEndpoint(),
          HttpMethod.POST,
          request,
          ClientLoginResponse.class
      );
      return responseService.success(responseEntity.getBody());

    } catch (HttpClientErrorException e) {
      log.error("Couldn't send client's token-grant to UAA.", e);
      throw mappingClientError(e);
    } catch (ResourceAccessException | IllegalStateException e) {
      log.error("Couldn't connect to UAA for sending client's token-grant.", e);
      throw new OAuthException("Couldn't connect to UAA for sending client's token-grant.", ErrorCodes.SOMETHING_WRONG);
    }
  }

  private String getTokenEndpoint() {
    return properties.getUaa().getAccessTokenUri();
  }

  private String getClientId() {
    String clientId = properties.getUaa().getClientId();
    if (clientId == null) {
      throw new OAuthException("ClientId hasn't been configured.", ErrorCodes.INVALID_GATEWAY_CLIENT);
    }
    return clientId;
  }

  private String getClientSecret() {
    String clientSecret = properties.getUaa().getClientSecret();
    if (clientSecret == null) {
      throw new OAuthException("ClientSecret hasn't been configured.", ErrorCodes.INVALID_GATEWAY_CLIENT);
    }
    return clientSecret;
  }

  private String getScopes() {
    String[] scopes = properties.getUaa().getScopes();
    if (scopes == null) {
      throw new OAuthException("Client's scopes haven't been configured.", ErrorCodes.INVALID_GATEWAY_CLIENT);
    }
    return StringUtils.join(scopes, " ");
  }

  private OAuthException mappingUserError(HttpClientErrorException e) {
    ObjectMapper mapper = new ObjectMapper();
    mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    if (HttpStatus.UNAUTHORIZED.equals(e.getStatusCode())) {
      return new OAuthException("Invalid refresh-token.", ErrorCodes.INVALID_REFRESH_TOKEN);
    }

    try {
      OAuth2ErrorResponse errorResponse = mapper.readValue(e.getResponseBodyAsString(), OAuth2ErrorResponse.class);
      switch (errorResponse.getError()) {
        case OAuth2Errors.INVALID_OTP:
          return new OAuthException("Invalid OTP.", ErrorCodes.INVALID_OTP);
        case OAuth2Errors.USER_DOES_NOT_EXISTS:
          return new OAuthException("User doesn't exists.", ErrorCodes.USER_DOES_NOT_EXISTS);
        case OAuth2Errors.INVALID_GRANT:
          return new OAuthException("Invalid password.", ErrorCodes.INVALID_PASSWORD);
        case OAuth2Errors.INIT_SESSION_FAILED:
          return new OAuthException("Init user's session failed.", ErrorCodes.INIT_SESSION_FAILED);
        case OAuth2Errors.USER_IS_NOT_ACTIVE:
          return new OAuthException("Current user isn't active.", ErrorCodes.USER_IS_NOT_ACTIVE);
        default:
          return new OAuthException("Couldn't mapping error response from UAA.", ErrorCodes.SOMETHING_WRONG);
      }
    } catch (JsonProcessingException ex) {
      return new OAuthException("Couldn't parse the error response from UAA.", ErrorCodes.SOMETHING_WRONG);
    }
  }

  private OAuthException mappingClientError(HttpClientErrorException e) {
    ObjectMapper mapper = new ObjectMapper();
    mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    String body = e.getResponseBodyAsString();

    if (HttpStatus.UNAUTHORIZED.equals(e.getStatusCode()) && StringUtils.isBlank(body)) {
      return new OAuthException("Invalid client.", ErrorCodes.INVALID_CLIENT);
    }

    try {
      OAuth2ErrorResponse errorResponse = mapper.readValue(body, OAuth2ErrorResponse.class);
      String error = errorResponse.getError();
      if (Objects.equals(error, OAuth2Errors.INVALID_SCOPE)) {
        return new OAuthException("Invalid scopes.", ErrorCodes.INVALID_SCOPES);
      } else if (Objects.equals(error, OAuth2Errors.INACTIVE_CLIENT)) {
        return new OAuthException("Client isn't active.", ErrorCodes.INACTIVE_CLIENT);
      } else if (Objects.equals(error, OAuth2Errors.CLIENT_DOES_NOT_EXISTS)) {
        return new OAuthException("Client doesn't exists.", ErrorCodes.CLIENT_DOES_NOT_EXISTS);
      } else {
        return new OAuthException("Couldn't mapping error response from UAA.", ErrorCodes.SOMETHING_WRONG);
      }
    } catch (JsonProcessingException ex) {
      return new OAuthException("Couldn't parse error response from UAA.", ErrorCodes.SOMETHING_WRONG);
    }
  }
}
