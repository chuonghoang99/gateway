package com.apus.gateway.filter;

import java.time.Duration;
import javax.servlet.http.HttpServletRequest;

import com.apus.gateway.configuration.GatewayProperties;
import com.apus.gateway.common.constant.IMaps;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.grid.hazelcast.HazelcastProxyManager;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;

/**
 * Zuul filter for limiting the number of HTTP calls per client.
 */
@Slf4j
public class RateLimitingFilter extends ZuulFilter {
  private final GatewayProperties gatewayProperties;
  private final HazelcastProxyManager<String> proxyManager;

  public RateLimitingFilter(GatewayProperties gatewayProperties, HazelcastInstance hazelcastInstance) {
    this.gatewayProperties = gatewayProperties;
    IMap<String, byte[]> map = hazelcastInstance.getMap(IMaps.RATE_LIMITING);
    proxyManager = new HazelcastProxyManager<>(map);
  }

  @Override
  public String filterType() {
    return "pre";
  }

  @Override
  public int filterOrder() {
    return 1;
  }

  @Override
  public boolean shouldFilter() {
    // specific APIs can be filtered out using
    return true;
  }

  @Override
  public Object run() {
    String bucketId = getId(RequestContext.getCurrentContext().getRequest());
    BucketConfiguration configuration = getConfiguration();
    Bucket bucket = proxyManager.builder().build(bucketId, configuration);

    if (bucket.tryConsume(1)) {
      // the limit is not exceeded
      log.debug("[Rate Limiting] API rate limit OK for {}", bucketId);
    } else {
      // limit is exceeded
      log.warn("[Rate Limiting] API rate limit exceeded for {}", bucketId);
      apiLimitExceeded();
    }
    return null;
  }

  private BucketConfiguration getConfiguration() {
    return BucketConfiguration.builder()
        .addLimit(Bandwidth.simple(gatewayProperties.getRateLimiting().getLimit(),
            Duration.ofSeconds(gatewayProperties.getRateLimiting().getDurationInSeconds())))
        .build();
  }

  /**
   * The ID that will identify the limit: the user login or the user IP address.
   */
  private String getId(@NonNull HttpServletRequest httpServletRequest) {
    return httpServletRequest.getRemoteAddr();
  }

  /**
   * Create a Zuul response error when the API limit is exceeded.
   */
  private void apiLimitExceeded() {
    RequestContext ctx = RequestContext.getCurrentContext();
    ctx.setResponseStatusCode(HttpStatus.TOO_MANY_REQUESTS.value());
    if (ctx.getResponseBody() == null) {
      ctx.setResponseBody("API rate limit exceeded");
      ctx.setSendZuulResponse(false);
    }
  }
}

