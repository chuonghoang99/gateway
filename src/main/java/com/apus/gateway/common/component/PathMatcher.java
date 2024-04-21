package com.apus.gateway.common.component;

import org.springframework.lang.NonNull;

public interface PathMatcher {

  boolean match(@NonNull String pattern, @NonNull String path);
}