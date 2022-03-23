package org.broadinstitute.listener.relay.wss;

import com.microsoft.azure.relay.HybridConnectionChannel;
import com.microsoft.azure.relay.HybridConnectionListener;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.HashMap;
import java.util.Map;
import org.broadinstitute.listener.relay.http.RelayedHttpRequest;
import org.broadinstitute.listener.relay.inspectors.InspectorsProcessor;
import org.broadinstitute.listener.relay.transport.TargetResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

@Component
public class WebSocketConnectionsHandler {

  private final HybridConnectionListener listener;
  private final Logger logger = LoggerFactory.getLogger(WebSocketConnectionsHandler.class);
  private final TargetResolver targetResolver;
  private final Map<String, RelayedHttpRequest> acceptedRequests;
  private final InspectorsProcessor inspectorsProcessor;

  public WebSocketConnectionsHandler(
      @NonNull HybridConnectionListener listener,
      @NonNull TargetResolver targetResolver,
      @NonNull InspectorsProcessor inspectorsProcessor) {
    this.listener = listener;
    this.targetResolver = targetResolver;
    this.inspectorsProcessor = inspectorsProcessor;
    acceptedRequests = new HashMap<>();
  }

  private synchronized RelayedHttpRequest addAcceptedRelayedRequest(
      String trackingId, @NonNull RelayedHttpRequest relayedHttpRequest) {
    acceptedRequests.put(trackingId, relayedHttpRequest);
    return relayedHttpRequest;
  }

  private synchronized RelayedHttpRequest getAcceptedRelayedRequest(String trackingId) {
    return acceptedRequests.getOrDefault(trackingId, null);
  }

  private synchronized void removeAcceptedRelayedRequest(String trackingId) {
    if (!acceptedRequests.containsKey(trackingId)) {
      acceptedRequests.remove(trackingId);
    }
  }

  public Flux<RelayedHttpRequest> acceptHttpUpgradeRequests() {

    return Flux.create(
        sink ->
            listener.setAcceptHandler(
                context -> {
                  try {

                    if (!inspectorsProcessor.isRelayedWebSocketUpgradeRequestAccepted(
                        context.getRequest())) {
                      logger.info(
                          "The WebSocket upgrade was rejected by an inspector. Tracking ID:{}",
                          context.getTrackingContext().getTrackingId());
                      return false;
                    }

                    RelayedHttpRequest request =
                        addAcceptedRelayedRequest(
                            context.getTrackingContext().getTrackingId(),
                            RelayedHttpRequest.createRelayedHttpRequest(context, targetResolver));
                    sink.next(request);
                  } catch (Exception e) {
                    logger.error("Failed to create a relayed http request", e);
                    return false;
                  }

                  return true;
                }));
  }

  private void acceptConnection(FluxSink<HybridConnectionChannel> sink) {
    if (listener.isOnline()) {

      listener
          .acceptConnectionAsync()
          .thenAcceptAsync(
              (connection) -> {
                // may be null when the listener is closed before receiving the connection
                if (connection != null) {
                  sink.next(connection);
                  acceptConnection(sink);
                  return;
                }

                sink.error(
                    new IllegalStateException(
                        "Invalid connection state. The connection was not Open"));
              });
    }
  }

  public Flux<HybridConnectionChannel> acceptConnections() {
    return Flux.create(this::acceptConnection);
  }

  public ConnectionsPair createLocalConnection(@NonNull HybridConnectionChannel relayedConnection) {

    try {
      return new ConnectionsPair(
          relayedConnection, createWebSocketFromRelayedHttpRequest(relayedConnection));

    } catch (Exception e) {
      logger.error("Error while creating the connection pair");
      throw new RuntimeException("Error creating the connection pair", e);
    }
  }

  private WebSocket createWebSocketFromRelayedHttpRequest(
      HybridConnectionChannel relayedConnection) {

    String trackingId = relayedConnection.getTrackingContext().getTrackingId();
    RelayedHttpRequest request = getAcceptedRelayedRequest(trackingId);

    if (request == null) {
      throw new IllegalStateException(
          "The request was not in the accepted list tracking id:" + trackingId);
    }

    WebSocket.Builder builder = HttpClient.newHttpClient().newWebSocketBuilder();

    // set cookies
    if (request.getHeaders().isPresent()) {
      if (request.getHeaders().get().containsKey("Cookie")) {
        builder.header("Cookie", request.getHeaders().get().get("Cookie"));
      }
    }

    return getWebSocketConnection(relayedConnection, trackingId, request, builder);
  }

  private WebSocket getWebSocketConnection(
      HybridConnectionChannel relayedConnection,
      String trackingId,
      RelayedHttpRequest request,
      WebSocket.Builder builder) {
    try {
      URI wsTargetUri = request.getTargetWebSocketUri();

      WebSocket ws =
          builder.buildAsync(wsTargetUri, new TargetWebSocketListener(relayedConnection)).get();
      removeAcceptedRelayedRequest(trackingId);

      logger.info("Successfully created target WebSocket connection. Tracking ID:{}", trackingId);

      return ws;
    } catch (Exception ex) {
      logger.error(
          "Error while opening target WebSocket connection. Tracking ID:{}", trackingId, ex);
      try {
        relayedConnection.close();
      } catch (IOException e) {
        logger.error("Failed to close caller connection.Tracking ID:{}", trackingId, ex);
      }
      throw new RuntimeException("Failed to create Target WebSocket.", ex);
    }
  }
}
