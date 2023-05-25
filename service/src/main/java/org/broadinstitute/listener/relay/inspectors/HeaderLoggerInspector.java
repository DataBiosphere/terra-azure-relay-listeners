package org.broadinstitute.listener.relay.inspectors;

import com.microsoft.azure.relay.RelayedHttpListenerRequest;
import com.microsoft.azure.relay.RelayedHttpListenerResponse;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.TreeMap;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.listener.relay.Utils;
import org.broadinstitute.listener.relay.inspectors.InspectorType.InspectorNameConstants;
import org.jetbrains.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component(InspectorNameConstants.HEADERS_LOGGER)
public class HeaderLoggerInspector implements RequestInspector {

  private final Logger logger = LoggerFactory.getLogger(HeaderLoggerInspector.class);
  private static final List<String> MUST_MASKED_HEADER_NAMES = List.of("Authorization", "Cookie");

  @Override
  public boolean inspectWebSocketUpgradeRequest(
      @NonNull RelayedHttpListenerRequest relayedHttpListenerRequest) {
    logRequest(relayedHttpListenerRequest, null, OffsetDateTime.now(), "WEBSOCKET_UPGRADE_REQUEST");
    return true;
  }

  @Override
  public boolean inspectRelayedHttpRequest(
      @NonNull RelayedHttpListenerRequest relayedHttpListenerRequest) {
    logRequest(relayedHttpListenerRequest, null, OffsetDateTime.now(), "HTTP_REQUEST");
    return true;
  }

  @VisibleForTesting
  public void logRequest(
      RelayedHttpListenerRequest relayedHttpListenerRequest,
      RelayedHttpListenerResponse relayedHttpListenerResponse,
      OffsetDateTime requestTimestamp,
      String prefix) {
    if (relayedHttpListenerResponse == null) {
      return;
    }

    // HTTP headers are case-insensitive
    var headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    headers.putAll(relayedHttpListenerRequest.getHeaders());

    var referer = headers.getOrDefault("Referer", "");
    var origin = headers.getOrDefault("Origin", "");
    var ua = headers.getOrDefault("User-Agent", "");
    var sub = getTokenClaim(relayedHttpListenerRequest.getHeaders(), "sub").orElse("");

    // get the idtyp so we can log if the request was made by a uami (i.e,. the value is "app")
    var idTyp = getTokenClaim(relayedHttpListenerRequest.getHeaders(), "idtyp").orElse("");

    String endpoint = "";
    if (relayedHttpListenerRequest.getRemoteEndPoint() != null) {
      endpoint = relayedHttpListenerRequest.getRemoteEndPoint().toString();
    }

    var contentLength = relayedHttpListenerResponse.getHeaders().getOrDefault("Content-Length", "");

    logger.info(
        "{} {} {} {} {} '{} {}' {} {} {} '{}' {}",
        relayedHttpListenerResponse.getStatusCode(),
        contentLength,
        prefix,
        sub,
        idTyp,
        relayedHttpListenerRequest.getHttpMethod(),
        relayedHttpListenerRequest.getUri(),
        requestTimestamp,
        referer,
        origin,
        ua,
        endpoint);

    logHeaders(relayedHttpListenerRequest.getHeaders());
  }

  private Optional<String> getTokenClaim(Map<String, String> headers, String claim) {
    if (headers == null) {
      logger.error("No auth headers found");
      return Optional.empty();
    }

    var rawToken = Utils.getToken(headers);
    if (rawToken.isEmpty()) {
      logger.error("No valid token found");
      return Optional.empty();
    } else {
      TokenChecker checker = new TokenChecker();
      return checker.getClaim(rawToken.get(), claim);
    }
  }

  private void logHeaders(Map<String, String> headers) {
    if (headers == null) {
      return;
    }

    for (Entry<String, String> header : headers.entrySet()) {
      String value = header.getValue();
      if (isMaskedValue(header.getKey())) {
        value = "*******";
      }
      logger.debug("Header: {} Value:{}", header.getKey(), value);
    }
  }

  private boolean isMaskedValue(String key) {
    for (String maskedHeader : MUST_MASKED_HEADER_NAMES) {
      if (StringUtils.containsIgnoreCase(key, maskedHeader)) {
        return true;
      }
    }

    return false;
  }
}
