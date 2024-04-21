package com.apus.gateway.configuration;

import com.apus.gateway.oauth2.JwtAccessTokenStore;
import com.apus.gateway.oauth2.OAuth2JwtAccessTokenConverter;
import com.apus.gateway.oauth2.OAuth2SignatureVerifier;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.web.filter.CorsFilter;

@Configuration
@EnableResourceServer
@RequiredArgsConstructor
public class SecurityConfiguration extends ResourceServerConfigurerAdapter {
  private final GatewayProperties gatewayProperties;
  private final CorsFilter corsFilter;

  @Override
  public void configure(HttpSecurity http) throws Exception {
    http
        .csrf()
        .disable()
        .addFilterBefore(corsFilter, CsrfFilter.class)
        .headers()
        .frameOptions()
        .disable()
        .and()
        .sessionManagement()
        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        .and()
        .authorizeRequests()
        .antMatchers("/**/**/public-api/**").permitAll()
        .antMatchers("/**/**/public-files/**").permitAll()
        .antMatchers("/**/**/v3/api-docs/**").permitAll()
        .antMatchers("/oauth/login").permitAll()
        .antMatchers("/oauth/otp-login").permitAll()
        .antMatchers("/oauth/refresh-token").permitAll()
        .antMatchers("/oauth/client-login").permitAll()
        .antMatchers("/management/health").permitAll()
        .antMatchers("/management/info").permitAll()
        .antMatchers("/v3/api-docs/**").permitAll()
        .antMatchers("/swagger-ui/**").permitAll()
        .antMatchers("/**").authenticated();
  }

  @Bean
  public TokenStore tokenStore(JwtAccessTokenConverter jwtAccessTokenConverter) {
    return new JwtAccessTokenStore(jwtAccessTokenConverter);
  }

  @Bean
  public JwtAccessTokenConverter jwtAccessTokenConverter(OAuth2SignatureVerifier signatureVerifierClient) {
    return new OAuth2JwtAccessTokenConverter(gatewayProperties, signatureVerifierClient);
  }
}
