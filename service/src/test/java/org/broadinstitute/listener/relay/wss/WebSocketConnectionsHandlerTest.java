package org.broadinstitute.listener.relay.wss;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.microsoft.azure.relay.HybridConnectionListener;
import com.microsoft.azure.relay.RelayedHttpListenerContext;
import com.microsoft.azure.relay.RelayedHttpListenerRequest;
import com.microsoft.azure.relay.TrackingContext;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import org.broadinstitute.listener.relay.InvalidRelayTargetException;
import org.broadinstitute.listener.relay.http.RelayedHttpRequest;
import org.broadinstitute.listener.relay.inspectors.InspectorsProcessor;
import org.broadinstitute.listener.relay.transport.TargetResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WebSocketConnectionsHandlerTest {

  private static final String TARGET_URL = "http://localhost:8080/g?a=a";
  private static final String TARGET_WS_URL = "ws://localhost:8080/";

  private static final String RELAY_URL = "https://relay.azure.com/connection/g?a=a";

  @Mock(answer = Answers.CALLS_REAL_METHODS)
  private HybridConnectionListener listener;

  @Mock private RelayedHttpListenerContext context;
  @Mock private TrackingContext trackingContext;
  @Mock private RelayedHttpListenerRequest listenerRequest;
  @Mock private TargetResolver targetHostResolver;
  @Mock private InspectorsProcessor inspectorsProcessor;

  private WebSocketConnectionsHandler webSocketConnectionsHandler;

  @BeforeEach
  void setUp() {
    webSocketConnectionsHandler =
        new WebSocketConnectionsHandler(listener, targetHostResolver, inspectorsProcessor);
  }

  @Test
  void acceptHttpUpgradeRequests_acceptedByInspectors()
      throws MalformedURLException, URISyntaxException, InvalidRelayTargetException {
    setUpRelayedHttpUpgradeRequestMock();
    when(inspectorsProcessor.isRelayedWebSocketUpgradeRequestAccepted(listenerRequest))
        .thenReturn(true);

    final RelayedHttpRequest[] relayedHttpRequests = new RelayedHttpRequest[1];
    webSocketConnectionsHandler
        .acceptHttpUpgradeRequests()
        .subscribe(r -> relayedHttpRequests[0] = r);
    boolean handlerReturn = listener.getAcceptHandler().apply(context);

    assertThat(relayedHttpRequests[0].getMethod(), equalTo("GET"));
    assertThat(handlerReturn, equalTo(true));
  }

  @Test
  void acceptHttpUpgradeRequests_rejectedByInspectors() {
    when(inspectorsProcessor.isRelayedWebSocketUpgradeRequestAccepted(listenerRequest))
        .thenReturn(false);

    final RelayedHttpRequest[] relayedHttpRequests = new RelayedHttpRequest[1];
    webSocketConnectionsHandler
        .acceptHttpUpgradeRequests()
        .subscribe(r -> relayedHttpRequests[0] = r);
    boolean handlerReturn = listener.getAcceptHandler().apply(context);

    assertThat(relayedHttpRequests[0], equalTo(null));
    assertThat(handlerReturn, equalTo(false));
  }

  private void setUpRelayedHttpUpgradeRequestMock()
      throws MalformedURLException, URISyntaxException, InvalidRelayTargetException {

    when(targetHostResolver.createTargetUrl(any())).thenReturn(new URL(TARGET_URL));
    when(targetHostResolver.createTargetWebSocketUri(any())).thenReturn(new URI(TARGET_WS_URL));

    when(listenerRequest.getHttpMethod()).thenReturn("GET");
    when(listenerRequest.getHeaders()).thenReturn(new HashMap<>());
    when(listenerRequest.getUri()).thenReturn(new URI(RELAY_URL));

    when(context.getRequest()).thenReturn(listenerRequest);
    when(context.getTrackingContext()).thenReturn(trackingContext);
    when(trackingContext.getTrackingId()).thenReturn("ID_1");
  }
}
