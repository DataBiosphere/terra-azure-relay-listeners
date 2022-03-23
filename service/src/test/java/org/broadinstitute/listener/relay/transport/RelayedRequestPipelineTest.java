package org.broadinstitute.listener.relay.transport;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.microsoft.azure.relay.RelayedHttpListenerContext;
import org.broadinstitute.listener.relay.http.ListenerConnectionHandler;
import org.broadinstitute.listener.relay.http.RelayedHttpRequestProcessor;
import org.broadinstitute.listener.relay.http.RelayedHttpRequestProcessor.Result;
import org.broadinstitute.listener.relay.http.TargetHttpResponse;
import org.broadinstitute.listener.relay.wss.WebSocketConnectionsHandler;
import org.broadinstitute.listener.relay.wss.WebSocketConnectionsRelayerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;

@ExtendWith(MockitoExtension.class)
class RelayedRequestPipelineTest {

  @Mock private RelayedHttpListenerContext requestContext;
  @Mock private TargetHttpResponse targetHttpResponse;
  @Mock private ListenerConnectionHandler listenerConnectionHandler;
  @Mock private RelayedHttpRequestProcessor relayedHttpRequestProcessor;
  @Mock private WebSocketConnectionsHandler webSocketConnectionsHandler;
  @Mock private WebSocketConnectionsRelayerService webSocketConnectionsRelayerService;

  private RelayedRequestPipeline relayedRequestPipeline;

  @BeforeEach
  void setUp() {
    relayedRequestPipeline =
        new RelayedRequestPipeline(
            listenerConnectionHandler,
            relayedHttpRequestProcessor,
            webSocketConnectionsHandler,
            webSocketConnectionsRelayerService);
  }

  @Test
  void registerHttpExecutionPipeline_isAcceptedByInspector() {
    when(listenerConnectionHandler.receiveRelayedHttpRequests())
        .thenReturn(Flux.create(s -> s.next(requestContext)));
    when(listenerConnectionHandler.isRelayedHttpRequestAcceptedByInspectors(any()))
        .thenReturn(true);
    when(relayedHttpRequestProcessor.executeRequestOnTarget(requestContext))
        .thenReturn(targetHttpResponse);
    when(relayedHttpRequestProcessor.writeTargetResponseOnCaller(targetHttpResponse))
        .thenReturn(Result.SUCCESS);

    relayedRequestPipeline.registerHttpExecutionPipeline();

    verify(relayedHttpRequestProcessor, times(1)).executeRequestOnTarget(requestContext);
    verify(relayedHttpRequestProcessor, times(1)).writeTargetResponseOnCaller(targetHttpResponse);
    verify(relayedHttpRequestProcessor, times(0)).writeNotAcceptedResponseOnCaller(any());
  }

  @Test
  void registerHttpExecutionPipeline_isNotAcceptedByInspector() {
    when(listenerConnectionHandler.receiveRelayedHttpRequests())
        .thenReturn(Flux.create(s -> s.next(requestContext)));
    when(listenerConnectionHandler.isRelayedHttpRequestAcceptedByInspectors(any()))
        .thenReturn(false);

    relayedRequestPipeline.registerHttpExecutionPipeline();

    verify(relayedHttpRequestProcessor, times(0)).executeRequestOnTarget(any());
    verify(relayedHttpRequestProcessor, times(0)).writeTargetResponseOnCaller(any());
    verify(relayedHttpRequestProcessor, times(1)).writeNotAcceptedResponseOnCaller(requestContext);
  }
}
