package org.broadinstitute.listener.config;

import java.util.List;
import java.util.stream.Collectors;

public record CorsSupportProperties(
    String preflightMethods,
    String allowHeaders,
    String maxAge,
    String contentSecurityPolicy,
    List<String> validHosts) {
  public CorsSupportProperties(
      String preflightMethods,
      String allowHeaders,
      String maxAge,
      String contentSecurityPolicy,
      List<String> validHosts) {
    this.preflightMethods = preflightMethods;
    this.allowHeaders = allowHeaders;
    this.maxAge = maxAge;
    this.contentSecurityPolicy = contentSecurityPolicy;
    this.validHosts = validHosts.stream().map(String::trim).collect(Collectors.toList());
  }
}
