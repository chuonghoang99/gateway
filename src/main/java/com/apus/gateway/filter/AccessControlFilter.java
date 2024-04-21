package com.apus.gateway.filter;

import com.apus.gateway.common.component.*;
import com.apus.gateway.configuration.GatewayProperties;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.netflix.zuul.filters.Route;
import org.springframework.cloud.netflix.zuul.filters.RouteLocator;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2Request;

import javax.servlet.http.HttpServletRequest;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Zuul filter for restricting access to backend microservices endpoints.
 */
@Slf4j
@RequiredArgsConstructor
public class AccessControlFilter extends ZuulFilter {
  private final RouteLocator routeLocator;
  private final GatewayProperties gatewayProperties;
  private final PathMatcher pathMatcher;
  private final RoleCache roleCache;
  private final ScopeCache scopeCache;

  @Override
  public String filterType() {
    return "pre";
  }

  @Override
  public int filterOrder() {
    return 0;
  }

  @Override
  public boolean shouldFilter() {
    HttpServletRequest request = RequestContext.getCurrentContext().getRequest();
    String requestUri = request.getRequestURI();
    String contextPath = request.getContextPath();
    String method = request.getMethod();

    log.info("[Access Control] : Incoming request on endpoint: {} [{}]", requestUri, method);

    // loop over registered routes
    for (Route route : routeLocator.getRoutes()) {
      String routePattern = contextPath + route.getFullPath();
      String serviceName = route.getId();
      String prefixPath = routePattern.substring(0, routePattern.length() - 2);
      // If the request is matching with a route
      if (requestUri.startsWith(prefixPath)) {
        return !isAuthorized(serviceName, prefixPath, method, requestUri);
      }
    }
    // denied access by filtering the illegal request
    return true;
  }

  @Override
  public Object run() {
    RequestContext ctx = RequestContext.getCurrentContext();
    log.warn("[Access Control] : Filtered unauthorized access on endpoint: {} [{}]",
        ctx.getRequest().getRequestURI(), ctx.getRequest().getMethod());
    ctx.setResponseStatusCode(HttpStatus.FORBIDDEN.value());
    ctx.setSendZuulResponse(false);
    return null;
  }

  private boolean isAuthorized(String serviceName, String prefixPath, String method, String requestUri) {
    String endpoint = requestUri.substring(prefixPath.length() - 1);

    for (String authorizedEndpoint : gatewayProperties.getAuthorizedEndpoints()) {
      if (pathMatcher.match(authorizedEndpoint, endpoint)) {
        return allowAccess(serviceName, endpoint, method);
      }
    }

    if (isAuthorizedByUaa(serviceName, endpoint, method)) {
      return allowAccess(serviceName, endpoint, method);
    }

    return false;
  }

  private boolean allowAccess(String serviceName, String endpoint, String method) {
    log.info("[Access Control] : Allow access to [{}] at endpoint: {} [{}]", serviceName, endpoint, method);
    return true;
  }

  /**
   * Call to UAA service for checking scopes of current client also roles of current user.
   *
   * @param serviceName
   * @param endpoint
   * @param method
   * @return
   */
  private boolean isAuthorizedByUaa(String serviceName, String endpoint, String method) {
    // get scopes and roles from security context
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    if (authentication instanceof OAuth2Authentication) {
      OAuth2Authentication oauth = (OAuth2Authentication) authentication;
      OAuth2Request request = oauth.getOAuth2Request();

      Set<String> scopes = request.getScope();
      List<String> authorities = authentication.getAuthorities().stream()
          .map(GrantedAuthority::getAuthority)
          .collect(Collectors.toList());
      ScopeKey key = new ScopeKey(serviceName, endpoint, method);

      // check scopes first
      if (!scopes.contains("gateway") && !hasIntersection(scopes, scopeCache.get(key))) {
        log.warn("[Access Control] : The client doesn't have enough scopes.");
        return false;
      }

      // then check authorities if existed
      if (authorities.contains(gatewayProperties.getSystemAdminRole())) {
        return true;
      } else {
        List<String> acceptedRoles = roleCache.get(key);
        // if the current endpoint is an authorized endpoint, so it just required the user is authenticated
        return acceptedRoles.contains(gatewayProperties.getSystemUserRole())
            || hasIntersection(authorities, acceptedRoles);
      }
    }

    return false;
  }

  private boolean hasIntersection(Collection<String> l1, Collection<String> l2) {
    for (String s : l1) {
      if (l2.contains(s)) {
        return true;
      }
    }
    return false;
  }
}
