package org.broadinstitute.listener.config;

public record CorsSupportProperties(String preflightMethods, String allowHeaders, String maxAge) {}
