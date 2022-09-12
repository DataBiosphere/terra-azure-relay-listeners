package org.broadinstitute.listener.config;

public record TargetRoutingRule(String pathContains, String targetHost, String removeFromPath) {}
