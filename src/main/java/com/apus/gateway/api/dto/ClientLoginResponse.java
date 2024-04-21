package com.apus.gateway.api.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Getter;
import lombok.Setter;

import java.util.Collection;
import java.util.Collections;

@Getter
@Setter
@JsonDeserialize(using = ClientLoginResponseDeserializer.class)
public class ClientLoginResponse {
  private String accessToken;
  private String tokenType;
  private Long expiresIn;
  private Collection<String> scopes;
  private String jti;

  public ClientLoginResponse() {
    scopes = Collections.emptySet();
  }
}
