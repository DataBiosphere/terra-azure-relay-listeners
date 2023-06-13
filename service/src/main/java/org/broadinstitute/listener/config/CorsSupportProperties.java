package org.broadinstitute.listener.config;

import java.util.List;

public record CorsSupportProperties(
    String preflightMethods,
    String allowHeaders,
    String maxAge,
    String contentSecurityPolicy,
    List<String> validHosts) {}
