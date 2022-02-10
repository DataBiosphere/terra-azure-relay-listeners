package org.broadinstitute.listener.relay.wss;

import com.microsoft.azure.relay.HybridConnectionChannel;
import com.microsoft.azure.relay.HybridConnectionListener;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.HashMap;
import java.util.Map;
import org.broadinstitute.listener.relay.http.RelayedHttpRequest;
import org.broadinstitute.listener.relay.transport.TargetHostResolver;
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
  private final TargetHostResolver targetHost;
  private final Map<String, RelayedHttpRequest> acceptedRequests;

  public WebSocketConnectionsHandler(
      @NonNull HybridConnectionListener listener, @NonNull TargetHostResolver targetHostResolver) {
    this.listener = listener;
    this.targetHost = targetHostResolver;
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
                    RelayedHttpRequest request =
                        addAcceptedRelayedRequest(
                            context.getTrackingContext().getTrackingId(),
                            RelayedHttpRequest.createRelayedHttpRequest(
                                context, targetHost.resolveTargetHost()));
                    sink.next(request);
                  } catch (Exception e) {
                    logger.error("Failed to create a relayed http request", e);
                    sink.error(e);
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

                // remove request from the list
                removeAcceptedRelayedRequest(connection.getTrackingContext().getTrackingId());
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

    return new ConnectionsPair(
        relayedConnection, createWebSocketFromRelayedHttpRequest(relayedConnection));
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

    try {
      URI wsTargetUri = request.getWebSocketTargetUri();
      WebSocket ws =
          builder.buildAsync(wsTargetUri, new TargetWebSocketListener(relayedConnection)).join();
      removeAcceptedRelayedRequest(trackingId);

      return ws;
    } catch (Exception ex) {
      logger.error("Error while opening local web socket connection", ex);
      throw ex;
    }
  }
}
