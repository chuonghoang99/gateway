package com.apus.gateway.configuration;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.CorsConfiguration;

import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
@Component
@ConfigurationProperties(
    prefix = "gateway",
    ignoreUnknownFields = false
)
public class GatewayProperties {
  private String systemAdminRole = "SYSTEM_ADMIN";
  private String systemUserRole = "SYSTEM_USER";
  private CorsConfiguration cors = new CorsConfiguration();
  private Cache cache = new Cache();
  private RateLimiting rateLimiting = new RateLimiting();
  private SignatureVerification signatureVerification = new SignatureVerification();
  private List<String> authorizedEndpoints = new ArrayList<>();
  private Uaa uaa = new Uaa();

  @Getter
  @Setter
  @NoArgsConstructor
  public static class Cache {
    private final Hazelcast hazelcast = new Hazelcast();
    private ApiCache roleCache = new ApiCache();
    private ApiCache scopeCache = new ApiCache();

    @Getter
    @Setter
    @NoArgsConstructor
    public static class Hazelcast {
      private int timeToLiveSeconds = 3600;
      private int maxIdleSeconds = 0;
      private int backupCount = 1;
      private int port = 5701;
      private int cpCountMember = 0;
      private boolean multicast = false;
      private boolean tcpIp = true;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class ApiCache {
      private boolean enabled = false;
      private int size = 1023;
      private long timeToLiveSeconds = 300;
    }
  }

  @Getter
  @Setter
  @NoArgsConstructor
  public static class Uaa {
    private String clientId;
    private String clientSecret;
    private String[] scopes;
    private String accessTokenUri = "http://uaa/oauth/token";
    private String acceptedRolesUri = "http://uaa/oauth/accepted-roles";
    private String acceptedScopesUri = "http://uaa/oauth/accepted-scopes";
    private String deleteRefreshTokenUri = "http://uaa/oauth/forced-logout";
  }

  @Getter
  @Setter
  @NoArgsConstructor
  public static class SignatureVerification {
    private String publicKeyEndpointUri = "http://uaa/oauth/token_key";
    /**
     * Maximum refresh rate for public keys in ms.
     * We won't fetch new public keys any faster than that to avoid spamming UAA in case
     * we receive a lot of "illegal" tokens.
     */
    private long publicKeyRefreshRateLimit = 10 * 1000L;
    /**
     * Maximum TTL for the public key in ms.
     * The public key will be fetched again from UAA if it gets older than that.
     * That way, we make sure that we get the newest keys always in case they are updated there.
     */
    private long ttl = 24 * 60 * 60 * 1000L;
  }

  @Getter
  @Setter
  @NoArgsConstructor
  public static class RateLimiting {
    private boolean enabled = false;
    private long limit = 1000000L;
    private int durationInSeconds = 3600;
  }
}
