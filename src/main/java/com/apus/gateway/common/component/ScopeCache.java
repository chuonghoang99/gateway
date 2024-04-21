package com.apus.gateway.common.component;

import com.apus.gateway.configuration.GatewayProperties;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class ScopeCache implements ApiCache<ScopeKey, String, List<String>> {
  private final GatewayProperties gatewayProperties;
  private final RestTemplate restTemplate;
  private final Cache<String, List<String>> cache;

  public ScopeCache(
      GatewayProperties gatewayProperties,
      @Qualifier("loadBalancedRestTemplate") RestTemplate restTemplate
  ) {
    this.gatewayProperties = gatewayProperties;
    this.restTemplate = restTemplate;

    // local cache, evict by LFU
    cache = Caffeine.newBuilder()
        .maximumSize(getSize())
        .expireAfterWrite(getTimeToLiveSeconds(), TimeUnit.SECONDS)
        .build();
  }

  @Override
  public List<String> getFromCache(String key) {
    return cache.getIfPresent(key);
  }

  @Override
  public void putToCache(String key, List<String> res) {
    cache.put(key, res);
  }

  @Override
  public boolean isEnabled() {
    return gatewayProperties.getCache().getScopeCache().isEnabled();
  }

  @Override
  public long getTimeToLiveSeconds() {
    return gatewayProperties.getCache().getScopeCache().getTimeToLiveSeconds();
  }

  @Override
  public int getSize() {
    return gatewayProperties.getCache().getScopeCache().getSize();
  }

  @Override
  public void clear() {
    cache.invalidateAll();
  }

  @Override
  public List<String> fetching(ScopeKey apiKey) {
    try {
      HttpEntity<Void> request = new HttpEntity<>(new HttpHeaders());
      List<String> acceptedScopes = restTemplate.exchange
          (
              getUri(apiKey),
              HttpMethod.GET,
              request,
              new ParameterizedTypeReference<List<String>>() {
              }
          ).getBody();
      return acceptedScopes != null ? acceptedScopes : new ArrayList<>();

    } catch (IllegalStateException | HttpClientErrorException e) {
      log.error("Could not connect to UAA for fetching accepted scopes.", e);
      return new ArrayList<>();
    }
  }

  @Override
  public String getUri(ScopeKey apiKey) {
    UriComponentsBuilder uriBuilder = UriComponentsBuilder
        .fromHttpUrl(gatewayProperties.getUaa().getAcceptedRolesUri())
        .queryParam("virtualHostName", apiKey.getVirtualHostName())
        .queryParam("path", apiKey.getPath())
        .queryParam("method", apiKey.getMethod());
    return uriBuilder.toUriString();
  }
}
