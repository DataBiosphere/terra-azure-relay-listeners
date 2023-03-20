package org.broadinstitute.listener.relay.inspectors;

import java.net.http.HttpClient;
import java.util.UUID;

public record SetDateAccessedInspectorOptions(
    String serviceHost,
    UUID workspaceId,
    int callWindowInSeconds,
    String runtimeName,
    HttpClient httpClient) {}
