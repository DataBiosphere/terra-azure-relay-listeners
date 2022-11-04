package org.broadinstitute.listener.relay.inspectors;

import com.microsoft.azure.relay.RelayedHttpListenerRequest;
import java.time.Instant;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
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
    var requestPath = relayedHttpListenerRequest.getUri().toString();
    if (requestPath.endsWith("status") || requestPath.endsWith("status/")) return true;
    else return checkPermission(relayedHttpListenerRequest.getHeaders());
  }

  private boolean checkPermission(Map<String, String> headers) {
    if (headers == null) {
      logger.error("No auth headers found");
      return false;
    }

    var leoToken = getToken(headers);

    if (leoToken.isEmpty()) {
      logger.error("No valid token found");
      return false;
    } else {
      var token = leoToken.get();
      return checkCachedPermission(token);
    }
  }

  public boolean checkCachedPermission(String accessToken) {
    var expiresAt = samResourceClient.checkWritePermission(accessToken);
    return expiresAt.isAfter(Instant.now());
  }

  protected Optional<String> getToken(Map<String, String> headers) {
    return getTokenFromCookie(headers).or(() -> Utils.getTokenFromAuthorization(headers));
  }

  protected Optional<String> getTokenFromCookie(Map<String, String> headers) {
    var cookieValue = headers.getOrDefault("cookie", headers.get("Cookie"));

    String[] splitted =
        Optional.ofNullable(cookieValue).map(s -> s.split(";")).orElse(new String[0]);

    return Arrays.stream(splitted)
        .filter(s -> s.contains(String.format("%s=", Utils.TOKEN_NAME)))
        .findFirst()
        .map(s -> s.split("=")[1]);
  }
}
