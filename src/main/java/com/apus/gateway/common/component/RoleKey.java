package com.apus.gateway.common.component;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class RoleKey implements ApiKey<String> {
  private String virtualHostName;
  private String path;
  private String method;

  @Override
  public String getKey() {
    return virtualHostName + path + method;
  }
}
