package com.apus.gateway.common.security;

import javax.servlet.http.HttpServletRequest;

public interface TokenExtractor {
  String extractToken(HttpServletRequest request);

  String extractHeaderToken(HttpServletRequest request);

  String extractRequestParamToken(HttpServletRequest request);
}
