package org.broadinstitute.listener.relay.inspectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.microsoft.azure.relay.RelayedHttpListenerRequest;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SetDateAccessedInspectorTest {

  private static final int CALL_WINDOW_IN_SECONDS = 5;
  private static final String SERVICE_HOST = "http://foo.bar";
  public static final String AUTH_TOKEN = "Bearer AUTH_TOKEN";
  public static final String AUTHORIZATION_HEADER = "Authorization";
  public static final String RUNTIME_NAME = "RUNTIME_NAME";
  @Mock private HttpClient httpClient;
  @Mock private RelayedHttpListenerRequest listenerRequest;
  @Mock private HttpResponse httpResponse;

  @Captor private ArgumentCaptor<HttpRequest> httpRequestArgumentCaptor;

  private SetDateAccessedInspectorOptions options;
  private SetDateAccessedInspector inspector;
  private UUID workspaceId;

  private Map<String, String> headers;

  @BeforeEach
  void setUp() throws IOException, URISyntaxException, InterruptedException {
    headers = new HashMap<>();
    headers.put(AUTHORIZATION_HEADER, AUTH_TOKEN);

    when(listenerRequest.getHeaders()).thenReturn(headers);

    when(httpClient.send(any(), any())).thenReturn(httpResponse);

    workspaceId = UUID.randomUUID();
    options =
        new SetDateAccessedInspectorOptions(
            SERVICE_HOST, workspaceId, CALL_WINDOW_IN_SECONDS, RUNTIME_NAME, httpClient);
    inspector = new SetDateAccessedInspector(options);
  }

  @Test
  void inspectWebSocketUpgradeRequest_calledTwice_onlyOneCallToHttpClient()
      throws IOException, InterruptedException {
    inspector.inspectWebSocketUpgradeRequest(listenerRequest);
    inspector.inspectWebSocketUpgradeRequest(listenerRequest);

    verify(httpClient, times(1)).send(any(), any());
  }

  @Test
  void inspectRelayedHttpRequest_calledTwice_onlyOneCallToHttpClient()
      throws IOException, InterruptedException {
    when(listenerRequest.getHttpMethod()).thenReturn("GET");
    when(listenerRequest.getUri()).thenReturn(URI.create("http://valid.com"));

    inspector.inspectRelayedHttpRequest(listenerRequest);
    inspector.inspectRelayedHttpRequest(listenerRequest);

    verify(httpClient, times(1)).send(any(), any());
  }

  @Test
  void inspectRelayedHttpRequest_multipleCallsOverTwoWindows_twiceCallToHttpClient()
      throws IOException, InterruptedException {
    when(listenerRequest.getHttpMethod()).thenReturn("GET");
    when(listenerRequest.getUri()).thenReturn(URI.create("http://valid.com"));

    inspector.inspectRelayedHttpRequest(listenerRequest);
    inspector.inspectRelayedHttpRequest(listenerRequest);
    inspector.inspectRelayedHttpRequest(listenerRequest);
    Thread.sleep(CALL_WINDOW_IN_SECONDS * 1000);
    inspector.inspectRelayedHttpRequest(listenerRequest);
    inspector.inspectRelayedHttpRequest(listenerRequest);
    inspector.inspectRelayedHttpRequest(listenerRequest);

    verify(httpClient, times(2)).send(any(), any());
  }

  @Test
  void inspectRelayedHttpRequest_callOnce_callToLeoHasAuthHeader()
      throws IOException, InterruptedException {
    when(listenerRequest.getHttpMethod()).thenReturn("GET");
    when(listenerRequest.getUri()).thenReturn(URI.create("http://valid.com"));

    inspector.inspectRelayedHttpRequest(listenerRequest);

    verify(httpClient, times(1)).send(httpRequestArgumentCaptor.capture(), any());

    assertThat(
        httpRequestArgumentCaptor.getValue().headers().map(),
        hasEntry(AUTHORIZATION_HEADER, List.of(AUTH_TOKEN)));
  }

  @Test
  void inspectRelayedHttpRequest_sendOperationThrows_returnTrue()
      throws IOException, InterruptedException {
    when(listenerRequest.getHttpMethod()).thenReturn("GET");
    when(listenerRequest.getUri()).thenReturn(URI.create("http://valid.com"));

    when(httpClient.send(any(), any())).thenThrow(RuntimeException.class);

    assertThat(inspector.inspectRelayedHttpRequest(listenerRequest), is(true));
  }
}
