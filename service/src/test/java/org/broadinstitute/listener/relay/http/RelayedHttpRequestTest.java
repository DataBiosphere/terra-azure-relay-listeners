package org.broadinstitute.listener.relay.http;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.when;

import com.microsoft.azure.relay.RelayedHttpListenerContext;
import com.microsoft.azure.relay.RelayedHttpListenerRequest;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import org.broadinstitute.listener.relay.InvalidRelayTargetException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RelayedHttpRequestTest {

  private static final String TARGET_HOST = "http://localhost:8080/";
  private static final String TARGET_HOST_HTTPS = "https://localhost:8080/";

  private static final String RELAY_HOST = "https://tom.foo.com/";
  private static final String HYBRID_CONN = "connection";
  private static final String TARGET_PATH = "/data1/data2";
  private static final String TARGET_QS = "?var1=foo&var2=bar";
  private static final String RELAY_REQUEST =
      RELAY_HOST + "/" + HYBRID_CONN + TARGET_PATH + TARGET_QS;
  private static final String EXPECTED_TARGET_HOST =
      TARGET_HOST + HYBRID_CONN + TARGET_PATH + TARGET_QS;
  private static final String EXPECTED_TARGET_HOST_HTTPS =
      TARGET_HOST_HTTPS + HYBRID_CONN + TARGET_PATH + TARGET_QS;

  @Mock private RelayedHttpListenerContext context;

  @Mock private RelayedHttpListenerRequest listenerRequest;

  @Mock private ByteArrayInputStream body;

  private Map<String, String> requestHeaders;

  @BeforeEach
  void setUp() {
    when(context.getRequest()).thenReturn(listenerRequest);
    requestHeaders = new HashMap<>();
    requestHeaders.put("HEAD1", "VALUE1");
    requestHeaders.put("HEAD2", "VALUE2");
  }

  @Test
  void getTargetUrl_fromRelayedRequest() throws URISyntaxException, InvalidRelayTargetException {
    when(listenerRequest.getHttpMethod()).thenReturn("GET");
    when(listenerRequest.getUri()).thenReturn(new URI(RELAY_REQUEST));

    RelayedHttpRequest request = RelayedHttpRequest.createRelayedHttpRequest(context, TARGET_HOST);

    assertThat(request.getTargetUrl().toString(), equalTo(EXPECTED_TARGET_HOST));
  }

  @Test
  void createRelayedHttpRequest_fromRequestWithBodyAndHeaders()
      throws InvalidRelayTargetException, URISyntaxException {

    when(listenerRequest.getHttpMethod()).thenReturn("POST");
    when(listenerRequest.getHeaders()).thenReturn(requestHeaders);
    when(listenerRequest.getInputStream()).thenReturn(body);
    when(listenerRequest.getUri()).thenReturn(new URI(RELAY_REQUEST));

    RelayedHttpRequest request = RelayedHttpRequest.createRelayedHttpRequest(context, TARGET_HOST);

    assertThat(request.getMethod(), equalTo("POST"));
    assertThat(request.getHeaders().get().entrySet(), equalTo(requestHeaders.entrySet()));
    assertThat(request.getBody().get(), equalTo(body));
  }

  @Test
  void createTargetUriFromRelayContext_fromGetRequestNoBody()
      throws URISyntaxException, InvalidRelayTargetException {
    when(listenerRequest.getHttpMethod()).thenReturn("GET");
    when(listenerRequest.getHeaders()).thenReturn(requestHeaders);
    when(listenerRequest.getUri()).thenReturn(new URI(RELAY_REQUEST));

    RelayedHttpRequest request = RelayedHttpRequest.createRelayedHttpRequest(context, TARGET_HOST);

    assertThat(request.getMethod(), equalTo("GET"));
    assertThat(request.getHeaders().get().entrySet(), equalTo(requestHeaders.entrySet()));
    assertThat(request.getBody().isPresent(), equalTo(false));
  }

  @Test
  void getWebSocketTargetUrl_fromHTTPRequest()
      throws URISyntaxException, InvalidRelayTargetException {
    when(listenerRequest.getHttpMethod()).thenReturn("GET");
    when(listenerRequest.getHeaders()).thenReturn(requestHeaders);
    when(listenerRequest.getUri()).thenReturn(new URI(RELAY_REQUEST));

    RelayedHttpRequest request = RelayedHttpRequest.createRelayedHttpRequest(context, TARGET_HOST);

    URI httpTargetUri = new URI(EXPECTED_TARGET_HOST);
    assertThat(request.getWebSocketTargetUri().getScheme(), equalTo("ws"));
    assertThat(request.getWebSocketTargetUri().getHost(), equalTo(httpTargetUri.getHost()));
    assertThat(request.getWebSocketTargetUri().getPath(), equalTo(httpTargetUri.getPath()));
    assertThat(request.getWebSocketTargetUri().getQuery(), equalTo(httpTargetUri.getQuery()));
  }

  @Test
  void getWebSocketTargetUrl_fromHTTPSRequest()
      throws URISyntaxException, InvalidRelayTargetException {
    when(listenerRequest.getHttpMethod()).thenReturn("GET");
    when(listenerRequest.getHeaders()).thenReturn(requestHeaders);
    when(listenerRequest.getUri()).thenReturn(new URI(RELAY_REQUEST));

    RelayedHttpRequest request =
        RelayedHttpRequest.createRelayedHttpRequest(context, TARGET_HOST_HTTPS);

    URI httpTargetUri = new URI(EXPECTED_TARGET_HOST_HTTPS);
    assertThat(request.getWebSocketTargetUri().getScheme(), equalTo("wss"));
    assertThat(request.getWebSocketTargetUri().getHost(), equalTo(httpTargetUri.getHost()));
    assertThat(request.getWebSocketTargetUri().getPath(), equalTo(httpTargetUri.getPath()));
    assertThat(request.getWebSocketTargetUri().getQuery(), equalTo(httpTargetUri.getQuery()));
  }
}
