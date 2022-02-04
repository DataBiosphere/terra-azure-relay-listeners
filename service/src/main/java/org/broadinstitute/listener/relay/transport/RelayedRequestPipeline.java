package org.broadinstitute.listener.relay.transport;

import org.broadinstitute.listener.relay.http.LocalHttpResponseWriter;
import org.broadinstitute.listener.relay.http.RelayedHttpRequestProcessor;
import org.broadinstitute.listener.relay.http.RelayedHttpRequestReceiver;
import org.broadinstitute.listener.relay.wss.WebSocketConnectionsHandler;
import org.broadinstitute.listener.relay.wss.WebSocketConnectionsRelayerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
public class RelayedRequestPipeline {

  private final RelayedHttpRequestReceiver httpRequestReceiver;
  private final RelayedHttpRequestProcessor httpRequestProcessor;
  private final LocalHttpResponseWriter httpResponseWriter;
  private final WebSocketConnectionsHandler webSocketConnectionsHandler;
  private final WebSocketConnectionsRelayerService webSocketConnectionsRelayerService;

  private final Logger logger = LoggerFactory.getLogger(RelayedRequestPipeline.class);

  public RelayedRequestPipeline(
      @NonNull RelayedHttpRequestReceiver requestReceiver,
      @NonNull RelayedHttpRequestProcessor requestExecutor,
      @NonNull LocalHttpResponseWriter responseWriter,
      @NonNull WebSocketConnectionsHandler wsReader,
      @NonNull WebSocketConnectionsRelayerService webSocketConnectionsRelayerService) {
    this.httpRequestReceiver = requestReceiver;
    this.httpRequestProcessor = requestExecutor;
    this.httpResponseWriter = responseWriter;

    this.webSocketConnectionsHandler = wsReader;
    this.webSocketConnectionsRelayerService = webSocketConnectionsRelayerService;
  }

  public void processRelayedRequests() {
    logger.info("Starting Relay Listener Processor");

    logger.info("Registering HTTP pipeline");
    httpRequestReceiver
        .receiveRelayedHttpRequests()
        .map(request -> httpRequestProcessor.executeLocalRequest(request))
        .map(localHttpResponse -> httpResponseWriter.writeLocalResponse(localHttpResponse))
        .subscribe(
            result -> logger.info("Processed request with the following result: {}", result));

    logger.info("Registering WebSocket upgrades pipeline");
    webSocketConnectionsHandler
        .acceptHttpUpgradeRequests()
        .subscribe(
            request -> logger.info("Accepted request. Target URI:{}", request.getTargetURL()));

    httpRequestReceiver
        .openConnection()
        .subscribe(
            result ->
                // we can't start accepting connections until the connection is open
                webSocketConnectionsHandler
                    .acceptConnections()
                    .map(
                        callerConnection ->
                            webSocketConnectionsHandler.createLocalConnection(callerConnection))
                    .subscribe(
                        connectionsPair ->
                            webSocketConnectionsRelayerService.startDataRelay(connectionsPair)));
  }
}
