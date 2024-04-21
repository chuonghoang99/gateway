package com.apus.gateway.oauth2;

import com.apus.gateway.common.constant.IMaps;
import com.apus.gateway.common.security.JwtTokenDecoder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.exceptions.InvalidTokenException;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;
import org.springframework.security.oauth2.provider.token.store.JwtTokenStore;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Slf4j
@Component
public class JwtAccessTokenStore extends JwtTokenStore {
  private HazelcastInstance hazelcastInstance;
  private JwtTokenDecoder jwtTokenDecoder;
  private IMap<String, Boolean> blacklist;

  @Autowired
  @Qualifier("myHazelcast")
  public void setHazelcastInstance(HazelcastInstance hazelcastInstance) {
    this.hazelcastInstance = hazelcastInstance;
  }

  @Autowired
  public void setJwtTokenDecoder(JwtTokenDecoder jwtTokenDecoder) {
    this.jwtTokenDecoder = jwtTokenDecoder;
  }

  @PostConstruct
  public void init() {
    blacklist = hazelcastInstance.getMap(IMaps.EXPIRED_ACCESS_TOKENS);
  }

  /**
   * Create a JwtTokenStore with this token enhancer (should be shared with the DefaultTokenServices if used).
   *
   * @param jwtTokenEnhancer
   */
  public JwtAccessTokenStore(JwtAccessTokenConverter jwtTokenEnhancer) {
    super(jwtTokenEnhancer);
  }

  @Override
  public OAuth2AccessToken readAccessToken(String tokenValue) {
    try {
      var payload = jwtTokenDecoder.getPayload(tokenValue);
      if (payload == null) {
        throw new InvalidTokenException("Invalid Access Token.");
      }
      var jti = payload.getJti();
      var isExpired = blacklist.get(jti);
      if (Boolean.TRUE.equals(isExpired)) {
        throw new InvalidTokenException("Invalid Access Token (Expired).");
      }
    } catch (JsonProcessingException e) {
      log.error("Can't decode access token payload when reading access token!", e);
      throw new InvalidTokenException("Invalid Access Token.");
    }
    return super.readAccessToken(tokenValue);
  }
}
