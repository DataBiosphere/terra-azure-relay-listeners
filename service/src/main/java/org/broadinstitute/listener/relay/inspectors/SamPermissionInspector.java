package org.broadinstitute.listener.relay.inspectors;

import com.microsoft.azure.relay.RelayedHttpListenerRequest;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import org.broadinstitute.listener.relay.inspectors.InspectorType.InspectorNameConstants;
import org.jetbrains.annotations.NotNull;
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

    var cookieValue = headers.getOrDefault("cookie", headers.get("Cookie"));

    var leoToken = getToken(cookieValue);

    if (leoToken.isEmpty()) {
      logger.error("No valid cookie found");
      return false;
    } else {
      var token = leoToken.get();
      return samResourceClient.checkCachedPermission(token);
    }
  }

  protected Optional<String> getToken(@NotNull String cookieValue) {
    String[] splitted = cookieValue.split(";");

    if(cookieValue.isEmpty())
      return Optional.empty();
    else
      return Arrays.stream(splitted)
        .filter(s -> s.contains("LeoToken"))
        .findFirst()
        .map(s -> s.split("=")[1]);
  }
}
