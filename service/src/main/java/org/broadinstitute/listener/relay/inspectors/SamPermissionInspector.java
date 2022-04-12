package org.broadinstitute.listener.relay.inspectors;

import com.microsoft.azure.relay.RelayedHttpListenerRequest;
import java.util.Arrays;
import java.util.Map;
import org.broadinstitute.dsde.workbench.client.sam.ApiClient;
import org.broadinstitute.dsde.workbench.client.sam.ApiException;
import org.broadinstitute.dsde.workbench.client.sam.api.ResourcesApi;
import org.broadinstitute.listener.config.ListenerProperties;
import org.broadinstitute.listener.relay.inspectors.InspectorType.InspectorNameConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component(InspectorNameConstants.SAM_CHECKER)
public class SamPermissionInspector implements RequestInspector {
  private final Logger logger = LoggerFactory.getLogger(SamPermissionInspector.class);
  private final String SAM_RESOURCE_TYPE = "controlled-application-private-workspace-resource";
  private final ApiClient samClient = new ApiClient();

  @Autowired private ListenerProperties listenerProperties;

  @Override
  public boolean inspectWebSocketUpgradeRequest(
      @NonNull RelayedHttpListenerRequest relayedHttpListenerRequest) {
    return checkPermission(relayedHttpListenerRequest.getHeaders());
  }

  @Override
  public boolean inspectRelayedHttpRequest(
      @NonNull RelayedHttpListenerRequest relayedHttpListenerRequest) {
    return checkPermission(relayedHttpListenerRequest.getHeaders());
  }

  private boolean checkPermission(Map<String, String> headers) {

    if (headers == null) {
      logger.info("No auth headers found");
      return false;
    }

    var cookieValue = headers.getOrDefault("cookie", headers.get("Cookie"));
    String[] splitted = cookieValue.split(";");

    var leoToken = Arrays.stream(splitted)
        .takeWhile(s -> s.contains("LeoToken"))
        .findFirst()
        .map(s -> s.split("=")[1]);

    if (leoToken.isEmpty()) {
      logger.info("No valid cookie found " + cookieValue);
      return false;
    } else {
      var token = leoToken.get();
      samClient.setAccessToken(token);
      samClient.setBasePath(listenerProperties.getSamUrl());
      var resourceApi = new ResourcesApi(samClient);
      try {
        return resourceApi.resourcePermissionV2(SAM_RESOURCE_TYPE, listenerProperties.getSamResourceId(), "write");
      } catch (ApiException e) {
        logger.info("Fail to check Sam permission", e);
        return false;
      }
    }
  }
}
