package com.apus.gateway.common.security;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.util.Base64;

@Slf4j
public class JwtTokenDecoder implements TokenDecoder {
  private final Base64.Decoder decoder;

  public JwtTokenDecoder() {
    decoder = Base64.getUrlDecoder();
  }

  @Override
  public String decodeHeader(@NonNull String token) {
    String[] chunks = token.split("\\.");
    return new String(decoder.decode(chunks[0]));
  }

  @Override
  @Nullable
  public String decodePayload(@NonNull String token) {
    String[] chunks = token.split("\\.");
    if (chunks.length <= 1) {
      log.error("Invalid token, can't get payload of the given token {}", token);
      return null;
    }
    return new String(decoder.decode(chunks[1]));
  }

  @Override
  @Nullable
  public TokenPayload getPayload(String token) throws JsonProcessingException {
    String payload = decodePayload(token);
    if (payload == null) {
      return null;
    }
    ObjectMapper mapper = new ObjectMapper();
    mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    return mapper.readValue(payload, TokenPayload.class);
  }

  @Override
  public RefreshTokenPayload getRefreshTokenPayload(String token) throws JsonProcessingException {
    String payload = decodePayload(token);
    if (payload == null) {
      return null;
    }
    ObjectMapper mapper = new ObjectMapper();
    mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    return mapper.readValue(payload, RefreshTokenPayload.class);
  }
}
