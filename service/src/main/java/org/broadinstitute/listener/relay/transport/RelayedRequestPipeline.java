package org.broadinstitute.listener.relay.transport;

import com.microsoft.azure.relay.HybridConnectionChannel;
import com.microsoft.azure.relay.RelayedHttpListenerContext;
import org.broadinstitute.listener.relay.http.ListenerConnectionHandler;
import org.broadinstitute.listener.relay.http.RelayedHttpRequestProcessor;
import org.broadinstitute.listener.relay.wss.ConnectionsPair;
import org.broadinstitute.listener.relay.wss.WebSocketConnectionsHandler;
import org.broadinstitute.listener.relay.wss.WebSocketConnectionsRelayerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import reactor.core.publisher.SynchronousSink;

@Component
public class RelayedRequestPipeline {

  private final ListenerConnectionHandler listenerConnectionHandler;
  private final RelayedHttpRequestProcessor httpRequestProcessor;
  private final WebSocketConnectionsHandler webSocketConnectionsHandler;
  private final WebSocketConnectionsRelayerService webSocketConnectionsRelayerService;

  private final Logger logger = LoggerFactory.getLogger(RelayedRequestPipeline.class);

  public RelayedRequestPipeline(
      @NonNull ListenerConnectionHandler listenerConnectionHandler,
      @NonNull RelayedHttpRequestProcessor relayedHttpRequestProcessor,
      @NonNull WebSocketConnectionsHandler webSocketConnectionsHandler,
      @NonNull WebSocketConnectionsRelayerService webSocketConnectionsRelayerService) {
    this.listenerConnectionHandler = listenerConnectionHandler;
    this.httpRequestProcessor = relayedHttpRequestProcessor;

    this.webSocketConnectionsHandler = webSocketConnectionsHandler;
    this.webSocketConnectionsRelayerService = webSocketConnectionsRelayerService;
  }

  public void processRelayedRequests() {
    logger.info("Starting Relay Listener Processor");

    logger.info("Registering HTTP pipeline");
    registerHttpExecutionPipeline();

    logger.info("Registering WebSocket upgrades pipeline");
    webSocketConnectionsHandler
        .acceptHttpUpgradeRequests()
        .subscribe(
            request -> logger.info("Accepted request. Target URI:{}", request.getTargetUrl()));

    openListenerConnection();
  }

  public void openListenerConnection() {
    listenerConnectionHandler
        .openConnection()
        .subscribe(
            result ->
                // we can't start accepting connections until the connection is open
                webSocketConnectionsHandler
                    .acceptConnections()
                    .handle(
                        (HybridConnectionChannel connectionChannel,
                            SynchronousSink<ConnectionsPair> sink) -> {
                          try {
                            ConnectionsPair connectionsPair =
                                webSocketConnectionsHandler.createLocalConnection(
                                    connectionChannel);
                            sink.next(connectionsPair);
                          } catch (Exception ex) {
                            logger.error("Error while creating the local connection", ex);
                          }
                        })
                    .subscribe(webSocketConnectionsRelayerService::startDataRelay));
  }

  public void registerHttpExecutionPipeline() {
    listenerConnectionHandler
        .receiveRelayedHttpRequests()
        .filter(c -> listenerConnectionHandler.isNotPreflight(c.getRequest()))
        .doOnDiscard(RelayedHttpListenerContext.class, httpRequestProcessor::writePreflightResponse)
        .filter(c -> listenerConnectionHandler.isNotSetCookie(c.getRequest()))
        .doOnDiscard(RelayedHttpListenerContext.class, httpRequestProcessor::writeSetCookieResponse)
        .filter(
            c -> listenerConnectionHandler.isRelayedHttpRequestAcceptedByInspectors(c.getRequest()))
        .doOnDiscard(
            RelayedHttpListenerContext.class,
            httpRequestProcessor::writeNotAcceptedResponseOnCaller)
        .map(httpRequestProcessor::executeRequestOnTarget)
        .map(httpRequestProcessor::writeTargetResponseOnCaller)
        .subscribe(
            result -> logger.info("Processed request with the following result: {}", result));
  }
}
