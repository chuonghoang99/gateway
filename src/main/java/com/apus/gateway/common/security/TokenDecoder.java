package com.apus.gateway.common.security;

import com.fasterxml.jackson.core.JsonProcessingException;

public interface TokenDecoder {

  String decodeHeader(String token);

  String decodePayload(String token);

  TokenPayload getPayload(String token) throws JsonProcessingException;

  RefreshTokenPayload getRefreshTokenPayload(String token) throws JsonProcessingException;
}
