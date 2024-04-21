package com.apus.gateway.common.security;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class RefreshTokenPayload extends TokenPayload {
  private String ati;
}
