package org.broadinstitute.listener.config;

import java.util.UUID;

public record SetDateAccessedInspectorProperties(
    String serviceHost, UUID workspaceId, int callWindowInSeconds, String runtimeName) {}
