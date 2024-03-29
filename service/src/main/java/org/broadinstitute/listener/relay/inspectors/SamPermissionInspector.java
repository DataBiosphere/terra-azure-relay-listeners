package org.broadinstitute.listener.relay.inspectors;

import com.microsoft.azure.relay.RelayedHttpListenerRequest;
import java.time.Instant;
import java.util.Map;
import org.broadinstitute.listener.relay.Utils;
import org.broadinstitute.listener.relay.inspectors.InspectorType.InspectorNameConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component(InspectorNameConstants.SAM_CHECKER)
public class SamPermissionInspector implements RequestInspector {
  private final Logger logger = LoggerFactory.getLogger(SamPermissionInspector.class);
  private final SamResourceClient samResourceClient;

  public SamPermissionInspector(SamResourceClient samResourceClient) {
    this.samResourceClient = samResourceClient;
  }

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
      logger.error("No auth headers found");
      return false;
    }

    var leoToken = Utils.getToken(headers);

    if (leoToken.isEmpty()) {
      logger.error("No valid token found");
      return false;
    } else {
      var token = leoToken.get();
      return checkCachedPermission(token);
    }
  }

  public boolean checkCachedPermission(String accessToken) {
    var expiresAt = samResourceClient.checkPermission(accessToken);
    return expiresAt.isAfter(Instant.now());
  }
}
