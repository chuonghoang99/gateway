package com.apus.gateway.oauth2;

import com.apus.gateway.configuration.GatewayProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.jwt.crypto.sign.RsaVerifier;
import org.springframework.security.jwt.crypto.sign.SignatureVerifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Objects;

/**
 * Client fetching the public key from UAA to create a {@link SignatureVerifier}.
 */
@Component
@Slf4j
public class UaaSignatureVerifier implements OAuth2SignatureVerifier {
  private final RestTemplate restTemplate;
  private final GatewayProperties gatewayProperties;

  public UaaSignatureVerifier(@Qualifier("loadBalancedRestTemplate") RestTemplate restTemplate,
                              GatewayProperties gatewayProperties) {
    this.restTemplate = restTemplate;
    this.gatewayProperties = gatewayProperties;
  }

  /**
   * Fetches the public key from the UAA.
   *
   * @return the public key used to verify JWT tokens; or {@code null}.
   */
  @Override
  public SignatureVerifier getSignatureVerifier() {
    try {
      HttpEntity<Void> request = new HttpEntity<>(new HttpHeaders());
      String key = (String) Objects.requireNonNull(
          restTemplate.exchange(getPublicKeyEndpoint(), HttpMethod.GET, request, Map.class).getBody()
      ).get("value");
      return new RsaVerifier(key);
    } catch (IllegalStateException | NullPointerException ex) {
      log.warn("Could not contact UAA to get public-key!", ex);
      return null;
    }
  }

  /**
   * Returns the configured endpoint URI to retrieve the public key.
   *
   * @return the configured endpoint URI to retrieve the public key.
   */
  private String getPublicKeyEndpoint() {
    return gatewayProperties.getSignatureVerification().getPublicKeyEndpointUri();
  }
}
