package com.apus.gateway.configuration;

import com.apus.gateway.common.component.PathMatcher;
import com.apus.gateway.common.component.RoleCache;
import com.apus.gateway.common.component.ScopeCache;
import com.apus.gateway.common.security.JwtTokenDecoder;
import com.apus.gateway.filter.AccessControlFilter;
import com.apus.gateway.filter.RateLimitingFilter;
import com.hazelcast.core.HazelcastInstance;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.client.loadbalancer.RestTemplateCustomizer;
import org.springframework.cloud.netflix.zuul.filters.RouteLocator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
@Slf4j
public class BeanConfiguration {

  @Bean
  public JwtTokenDecoder jwtTokenDecoder() {
    return new JwtTokenDecoder();
  }

  @Bean
  public CorsFilter corsFilter(GatewayProperties gatewayProperties) {
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    CorsConfiguration config = gatewayProperties.getCors();
    if (!CollectionUtils.isEmpty(config.getAllowedOrigins())) {
      log.info("Registering CORS filter!");
      source.registerCorsConfiguration("/**", config);
    }
    return new CorsFilter(source);
  }

  @Bean
  @Qualifier("loadBalancedRestTemplate")
  public RestTemplate loadBalancedRestTemplate(RestTemplateCustomizer customizer) {
    RestTemplate restTemplate = new RestTemplate();
    customizer.customize(restTemplate);
    return restTemplate;
  }

  @Bean
  public AccessControlFilter accessControlFilter(RouteLocator routeLocator,
                                                 GatewayProperties gatewayProperties,
                                                 PathMatcher pathMatcher,
                                                 RoleCache roleCache,
                                                 ScopeCache scopeCache) {
    return new AccessControlFilter(routeLocator, gatewayProperties, pathMatcher, roleCache, scopeCache);
  }

  @Bean
  @ConditionalOnProperty(value = "gateway.rate-limiting.enabled", havingValue = "true")
  public RateLimitingFilter rateLimitingFilter(GatewayProperties gatewayProperties,
                                               @Qualifier("myHazelcast") HazelcastInstance hazelcastInstance) {
    return new RateLimitingFilter(gatewayProperties, hazelcastInstance);
  }
}
