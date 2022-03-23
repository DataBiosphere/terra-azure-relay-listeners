package org.broadinstitute.listener.relay.inspectors;

import com.microsoft.azure.relay.RelayedHttpListenerRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;

public class InspectorsProcessor {
  private final List<RequestInspector> inspectors;
  private final Logger logger = LoggerFactory.getLogger(InspectorsProcessor.class);

  public InspectorsProcessor(@NonNull List<RequestInspector> inspectors) {
    this.inspectors = inspectors;
  }

  /**
   * Returns true if all inspectors accept the websocket upgrade request. False is returned if at
   * least one inspector did not accept the request.
   *
   * @param listenerRequest
   * @return true or false
   */
  public boolean isRelayedWebSocketUpgradeRequestAccepted(
      @NonNull RelayedHttpListenerRequest listenerRequest) {
    return isRequestAccepted(
        request ->
            applyInspectorsToWebSocketUpgradeRequest(request).stream().collect(Collectors.toSet()),
        listenerRequest);
  }

  /**
   * Returns true if all inspectors accept the HTTP request. False is returned if at least one
   * inspector did not accept the request.
   *
   * @param listenerRequest
   * @return true or false
   */
  public boolean isRelayedHttpRequestAccepted(@NonNull RelayedHttpListenerRequest listenerRequest) {
    return isRequestAccepted(
        request ->
            applyInspectorsToRelayedHttpRequest(request).stream().collect(Collectors.toSet()),
        listenerRequest);
  }

  private boolean isRequestAccepted(
      Function<RelayedHttpListenerRequest, Set<Boolean>> distinctSet,
      RelayedHttpListenerRequest listenerRequest) {
    Set<Boolean> distinctResults = distinctSet.apply(listenerRequest);
    boolean result = false;
    if (distinctResults.size() == 1) {
      result = distinctResults.iterator().next();
    }

    logger.info(
        "Inspection result for the HTTP request. Result: {}, URI:{}",
        result,
        listenerRequest.getUri());

    return result;
  }

  private List<Boolean> applyInspectorsToRelayedHttpRequest(
      RelayedHttpListenerRequest listenerRequest) {
    List<Boolean> results = new ArrayList<>();

    if (!inspectors.isEmpty()) {
      inspectors.forEach(
          requestInspector ->
              results.add(requestInspector.inspectRelayedHttpRequest(listenerRequest)));
    }

    return results;
  }

  private List<Boolean> applyInspectorsToWebSocketUpgradeRequest(
      RelayedHttpListenerRequest listenerRequest) {
    List<Boolean> results = new ArrayList<>();

    if (!inspectors.isEmpty()) {
      inspectors.forEach(
          requestInspector ->
              results.add(requestInspector.inspectWebSocketUpgradeRequest(listenerRequest)));
    }

    return results;
  }
}
