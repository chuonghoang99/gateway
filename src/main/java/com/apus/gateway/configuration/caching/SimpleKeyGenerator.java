package com.apus.gateway.configuration.caching;

import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.lang.NonNull;

import java.lang.reflect.Method;

public class SimpleKeyGenerator implements KeyGenerator {

  @Override
  public Object generate(@NonNull Object target, @NonNull Method method, @NonNull Object... params) {
    return new SimpleKey(target, method, params).toString();
  }
}
