package com.apus.gateway.oauth2;

import com.apus.gateway.configuration.GatewayProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.jwt.crypto.sign.SignatureVerifier;
import org.springframework.security.oauth2.common.exceptions.InvalidTokenException;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;

import java.util.Map;

/**
 * Improved {@link JwtAccessTokenConverter} that can handle lazy fetching of public verifier keys.
 */
@Slf4j
public class OAuth2JwtAccessTokenConverter extends JwtAccessTokenConverter {
  private final GatewayProperties gatewayProperties;
  private final OAuth2SignatureVerifier signatureVerifier;
  /**
   * When did we last fetch the public key?
   */
  private long lastKeyFetchTimestamp;

  public OAuth2JwtAccessTokenConverter(GatewayProperties gatewayProperties,
                                       OAuth2SignatureVerifier signatureVerifier) {
    this.gatewayProperties = gatewayProperties;
    this.signatureVerifier = signatureVerifier;

    tryCreateSignatureVerifier();
  }

  /**
   * Try to decode the token with the current public key.
   * If it fails, contact the OAuth2 server to get a new public key, then try again.
   * We might not have fetched it in the first place, or it might have changed.
   *
   * @param token the JWT token to decode.
   * @return the resulting claims.
   * @throws InvalidTokenException if we cannot decode the token.
   */
  @Override
  protected Map<String, Object> decode(String token) {
    try {
      //check if our public key and thus SignatureVerifier have expired
      long ttl = gatewayProperties.getSignatureVerification().getTtl();
      if (ttl > 0 && System.currentTimeMillis() - lastKeyFetchTimestamp > ttl) {
        throw new InvalidTokenException("Public key is expired!");
      }
      return super.decode(token);
    } catch (InvalidTokenException ex) {
      if (tryCreateSignatureVerifier()) {
        return super.decode(token);
      }
      throw ex;
    }
  }

  /**
   * Fetch a new public key from the AuthorizationServer.
   *
   * @return true, if we could fetch it; false, if we could not.
   */
  private boolean tryCreateSignatureVerifier() {
    long t = System.currentTimeMillis();
    if (t - lastKeyFetchTimestamp < gatewayProperties.getSignatureVerification().getPublicKeyRefreshRateLimit()) {
      return false;
    }
    try {
      SignatureVerifier verifier = signatureVerifier.getSignatureVerifier();
      if (verifier != null) {
        setVerifier(verifier);
        lastKeyFetchTimestamp = t;
        log.info("Public key retrieved from OAuth2 server to create SignatureVerifier!");
        return true;
      }
    } catch (Exception ex) {
      log.error("Could not get public key from OAuth2 server to create SignatureVerifier!", ex);
    }
    return false;
  }
}
