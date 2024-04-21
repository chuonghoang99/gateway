package com.apus.gateway.common.component;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import javax.annotation.PostConstruct;

@Component
public class AdvanceAntMatcher extends AntPathMatcher implements PathMatcher {
  private Cache<String, Boolean> cache;

  @PostConstruct
  public void init() {
    // local cache, max 1023 items, evict by LFU
    cache = Caffeine.newBuilder()
        .maximumSize(1023)
        .build();

    super.setCachePatterns(false);
    super.setCaseSensitive(true);
  }

  @Override
  public boolean match(@NonNull String pattern, @NonNull String path) {
    var key = pattern + "#" + path;
    var result = cache.getIfPresent(key);
    if (result == null) {
      result = super.match(pattern, path);
      cache.put(key, result);
    }
    return result;
  }
}
