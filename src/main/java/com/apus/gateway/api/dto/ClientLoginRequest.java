package com.apus.gateway.api.dto;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import java.util.Set;

@Getter
@Setter
public class ClientLoginRequest {
  @NotBlank
  private String clientId;
  @NotBlank
  private String clientSecret;
  private Set<String> scopes;
}
