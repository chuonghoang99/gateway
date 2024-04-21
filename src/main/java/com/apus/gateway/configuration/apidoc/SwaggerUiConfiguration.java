package com.apus.gateway.configuration.apidoc;

import com.apus.gateway.common.security.SecurityUtils;

import java.util.Collections;

import lombok.RequiredArgsConstructor;
import org.springdoc.core.AbstractSwaggerUiConfigProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@Profile("!prod")
public class SwaggerUiConfiguration {
  private final DiscoveryClient discoveryClient;
  @Value("${springdoc.swagger-ui.ssl}")
  private Boolean ssl;

  @GetMapping("/swagger-ui/swagger-config.json")
  public Map<String, Object> swaggerConfig() {
    var request = SecurityUtils.getCurrentRequest();
    if (request == null) {
      return Collections.emptyMap();
    }
    String baseUrl = request.getRequestURL().toString().replace(request.getRequestURI(), "");
    if (Boolean.TRUE.equals(ssl)) {
      baseUrl = baseUrl.replace("http", "https");
    }
    List<AbstractSwaggerUiConfigProperties.SwaggerUrl> urls = new LinkedList<>();
    for (String serviceName : discoveryClient.getServices()) {
      if (serviceName.contains("gateway")) {
        urls.add(
            new AbstractSwaggerUiConfigProperties.SwaggerUrl(serviceName,
                baseUrl + "/v3/api-docs",
                serviceName
            )
        );
      } else {
        urls.add(
            new AbstractSwaggerUiConfigProperties.SwaggerUrl(serviceName,
                baseUrl + "/services/" + serviceName + "/v3/api-docs",
                serviceName
            )
        );
      }
    }
    return Map.of("urls", urls);
  }
}
