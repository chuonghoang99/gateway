package com.apus.gateway.common.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;

import javax.servlet.http.HttpServletRequest;
import java.util.Enumeration;

@Slf4j
public class BearerTokenExtractor implements TokenExtractor {

  @Override
  public String extractToken(HttpServletRequest request) {
    log.debug("Trying to find token in header.");
    String token = extractHeaderToken(request);
    if (token == null) {
      log.debug("Token not found in headers. Trying to find in request parameters.");
      token = extractRequestParamToken(request);
      if (token == null) {
        log.debug("Token not found in request parameters. Not an OAuth2 request.");
      }
    }
    return token;
  }

  @Override
  public String extractHeaderToken(@NonNull HttpServletRequest request) {
    Enumeration<String> headers = request.getHeaders("Authorization");

    String value;
    do {
      if (!headers.hasMoreElements()) {
        return null;
      }

      value = headers.nextElement();
    } while (!value.toLowerCase().startsWith("Bearer".toLowerCase()));

    String token = value.substring("Bearer".length()).trim();

    int commaIndex = token.indexOf(44);
    if (commaIndex > 0) {
      token = token.substring(0, commaIndex);
    }
    return token;
  }

  @Override
  public String extractRequestParamToken(@NonNull HttpServletRequest request) {
    return request.getParameter("access_token");
  }
}
