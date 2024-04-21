package com.apus.gateway.common.security;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.Base64Utils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;

public final class SecurityUtils {
  private static final Logger log = LoggerFactory.getLogger(SecurityUtils.class);
  private static final BearerTokenExtractor tokenExtractor = new BearerTokenExtractor();
  private static final JwtTokenDecoder jwtTokenDecoder = new JwtTokenDecoder();

  private SecurityUtils() {

  }

  @Nullable
  public static Long getUserId() {
    TokenPayload tokenPayload = getTokenPayload();
    return tokenPayload == null ? null : tokenPayload.getUserId();
  }

  @Nullable
  public static String getAccessToken() {
    HttpServletRequest request = getCurrentRequest();
    return request == null ? null : tokenExtractor.extractToken(request);
  }

  @Nullable
  public static TokenPayload getTokenPayload() {
    var token = getAccessToken();
    if (token == null) {
      return null;
    }
    try {
      return jwtTokenDecoder.getPayload(token);
    } catch (JsonProcessingException ex) {
      log.error("Can't decode token payload!", ex);
      return null;
    }
  }

  @Nullable
  public static HttpServletRequest getCurrentRequest() {
    RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
    if (requestAttributes instanceof ServletRequestAttributes) {
      return ((ServletRequestAttributes)requestAttributes).getRequest();
    }
    log.warn("Not called in the context of an HTTP request");
    return null;
  }

  public static void addClientBasicAuth(@NonNull HttpHeaders reqHeaders, String clientId, String clientSecret) {
    reqHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
    String authorization = clientId + ":" + clientSecret;
    String basicAuth = "Basic " + Base64Utils.encodeToString(authorization.getBytes(StandardCharsets.UTF_8));
    reqHeaders.add("Authorization", basicAuth);
  }
}
