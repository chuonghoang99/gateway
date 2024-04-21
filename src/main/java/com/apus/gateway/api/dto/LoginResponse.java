package com.apus.gateway.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Getter;
import lombok.Setter;

import java.util.Collection;
import java.util.Collections;

@Getter
@Setter
@JsonDeserialize(using = LoginResponseDeserializer.class)
public class LoginResponse {
  private String accessToken;
  private String tokenType;
  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String refreshToken;
  private Long expiresIn;
  private Collection<String> scopes;
  private Long userId;
  private String jti;

  public LoginResponse() {
    scopes = Collections.emptySet();
  }
}
