package com.apus.gateway.configuration.caching;

import lombok.AllArgsConstructor;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;

@AllArgsConstructor
public class SimpleKey {
  private final Object target;
  private final Method method;
  private final Object[] params;

  @Override
  public String toString() {
    return target.getClass().getSimpleName() + "_"
        + method.getName() + "_"
        + StringUtils.arrayToCommaDelimitedString(this.params);
  }
}
