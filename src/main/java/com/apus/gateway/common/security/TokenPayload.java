package com.apus.gateway.common.security;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class TokenPayload implements Serializable {
  @JsonProperty("user_name")
  private String username;
  @JsonProperty("scope")
  private List<String> scopes;
  @JsonProperty("authorities")
  private List<String> authorities;
  @JsonProperty("user_id")
  private Long userId;
  private Long exp;
  private String jti;
  @JsonProperty("client_id")
  private String clientId;
  @JsonProperty("tenant_id")
  private Long tenantId;
  @JsonProperty("company_id")
  private String companyId;
  @JsonProperty("branch_id")
  private String branchId;
}
