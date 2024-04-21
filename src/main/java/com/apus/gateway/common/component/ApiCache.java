package com.apus.gateway.common.component;

public interface ApiCache <K extends ApiKey<T>, T, V> {

  default V get(K apiKey) {
    if (!isEnabled()) {
      return fetching(apiKey);
    }

    T key = apiKey.getKey();
    V res = getFromCache(key);

    if (res == null) {
      res = fetching(apiKey);
      putToCache(key, res);
    }

    return res;
  }

  V getFromCache(T key);

  void putToCache(T key, V res);

  boolean isEnabled();

  long getTimeToLiveSeconds();

  int getSize();

  void clear();

  V fetching(K apiKey);

  String getUri(K apiKey);
}
