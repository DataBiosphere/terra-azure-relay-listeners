package org.broadinstitute.listener.relay.inspectors;

import com.microsoft.azure.relay.RelayedHttpListenerRequest;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.listener.relay.inspectors.InspectorType.InspectorNameConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component(InspectorNameConstants.HEADERS_LOGGER)
public class HeaderLoggerInspector implements RequestInspector {

  private final Logger logger = LoggerFactory.getLogger(HeaderLoggerInspector.class);

  @Override
  public boolean inspectWebSocketUpgradeRequest(
      @NonNull RelayedHttpListenerRequest relayedHttpListenerRequest) {

    logger.info("WebSocket Upgrade Request: {}", relayedHttpListenerRequest.getUri());

    logHeaders(relayedHttpListenerRequest.getHeaders());

    return true;
  }

  @Override
  public boolean inspectRelayedHttpRequest(
      @NonNull RelayedHttpListenerRequest relayedHttpListenerRequest) {

    logger.info("HTTP Request: {}", relayedHttpListenerRequest.getUri());

    logHeaders(relayedHttpListenerRequest.getHeaders());

    return true;
  }

  private void logHeaders(Map<String, String> headers) {

    if (headers == null) {
      return;
    }

    for (Entry<String, String> header : headers.entrySet()) {
      String value = header.getValue();
      if (StringUtils.containsIgnoreCase(header.getKey(), "Auhtorization")) {
        value = "*******";
      }
      logger.info("Header: {} Value:{}", header.getKey(), value);
    }
  }
}
