package org.broadinstitute.listener.config;

public record SamInspectorProperties(
    String samUrl, String samResourceId, String samResourceType, String samAction) {}
