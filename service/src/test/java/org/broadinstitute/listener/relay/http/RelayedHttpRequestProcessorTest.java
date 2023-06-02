package org.broadinstitute.listener.relay.http;

import static com.google.common.net.HttpHeaders.CONTENT_SECURITY_POLICY;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.microsoft.azure.relay.RelayedHttpListenerContext;
import com.microsoft.azure.relay.RelayedHttpListenerRequest;
import com.microsoft.azure.relay.RelayedHttpListenerResponse;
import com.microsoft.azure.relay.TrackingContext;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import org.broadinstitute.listener.config.CorsSupportProperties;
import org.broadinstitute.listener.relay.InvalidRelayTargetException;
import org.broadinstitute.listener.relay.http.RelayedHttpRequestProcessor.Result;
import org.broadinstitute.listener.relay.inspectors.GoogleTokenInfoClient;
import org.broadinstitute.listener.relay.inspectors.TokenChecker;
import org.broadinstitute.listener.relay.transport.TargetResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RelayedHttpRequestProcessorTest {

  private static final String BODY_CONTENT = "BODY";
  private static final String TARGET_URL = "http://localhost:8080/g?a=a";
  private static final String TARGET_WS_URL = "ws://localhost:8080/";
  private static final String RELAY_URL = "https://relay.azure.com/connection/g?a=a";

  private ByteArrayInputStream body;
  private RelayedHttpRequestProcessor processor;

  @Mock private HttpClient httpClient;

  @Mock private InputStream mockBody;
  @Mock private RelayedHttpListenerContext context;
  @Mock private RelayedHttpListenerRequest listenerRequest;
  @Mock private RelayedHttpRequest request;
  @Mock private HttpResponse targetClientResponse;
  @Mock private HttpHeaders targetResponseHttpHeaders;
  @Mock private RelayedHttpListenerResponse listenerResponse;
  @Mock private OutputStream responseStream;
  @Mock private TargetHttpResponse targetHttpResponse;
  @Mock private TargetResolver targetHostResolver;
  @Mock private TrackingContext trackingContext;
  @Captor private ArgumentCaptor<byte[]> responseData;

  private Map<String, List<String>> targetResponseHeaders;
  private Map<String, String> requestHeaders;

  private Map<String, String> requestHeaders_invalidOrigin;

  @BeforeEach
  void setUp() {
    body = new ByteArrayInputStream(BODY_CONTENT.getBytes(StandardCharsets.UTF_8));
    targetResponseHeaders = new HashMap<>();
    targetResponseHeaders.put("RES_HEADER", List.of("RES_VALUE"));
    requestHeaders = new HashMap<>();
    requestHeaders.put("REQ_HEADER", "REQ_VALUE");
    requestHeaders.put("Origin", "app.terra.bio");

    requestHeaders_invalidOrigin = new HashMap<>();
    requestHeaders_invalidOrigin.put("Origin", "malicious.website.com");

    List<String> validHosts =
        new ArrayList<>() {
          {
            add("app.terra.bio");
          }
        };
    processor =
        new RelayedHttpRequestProcessor(
            httpClient,
            targetHostResolver,
            new CorsSupportProperties("dummy", "dummy", "dummy", "dummy", validHosts),
            new TokenChecker(new GoogleTokenInfoClient()));
  }

  @Test
  void executeRequestOnTarget_successfullyExecuted()
      throws IOException, InterruptedException, URISyntaxException, InvalidRelayTargetException {
    setUpRelayedHttpRequestMock();
    when(targetClientResponse.statusCode()).thenReturn(200);
    when(httpClient.send(any(), any())).thenReturn(targetClientResponse);
    when(targetClientResponse.headers()).thenReturn(targetResponseHttpHeaders);
    when(targetResponseHttpHeaders.map()).thenReturn(targetResponseHeaders);
    when(targetClientResponse.body()).thenReturn(body);

    TargetHttpResponse response = processor.executeRequestOnTarget(context);
    String data = new String(response.getBody().get().readAllBytes());

    assertThat(response.getStatusCode(), equalTo(200));
    targetResponseHeaders.put(CONTENT_SECURITY_POLICY, List.of("dummy"));

    // check if response headers from the target were included in the listener's response
    for (Entry<String, List<String>> entry : targetResponseHttpHeaders.map().entrySet()) {
      assertThat(
          response.getHeaders().get(),
          hasEntry(
              entry.getKey(),
              entry.getValue().stream().findFirst().get())); // multi-part headers are not supported
    }

    assertThat(data, equalTo(BODY_CONTENT));
  }

  @Test
  void executeRequestOnTarget_failedToExecute()
      throws IOException, InterruptedException, URISyntaxException, InvalidRelayTargetException {
    setUpRelayedHttpRequestMock();
    when(httpClient.send(any(), any())).thenThrow(new InterruptedException("Error Msg"));
    when(context.getTrackingContext()).thenReturn(trackingContext);
    when(trackingContext.getTrackingId()).thenReturn("ID_1");
    TargetHttpResponse response = processor.executeRequestOnTarget(context);

    assertThat(response.getStatusCode(), equalTo(500));
    assertThat(response.getStatusDescription(), equalTo("Error Msg"));
    assertThat(response.getBody().isPresent(), equalTo(true));
  }

  @Test
  void writeTargetResponseOnCaller_responseIsWrittenBackToCaller() throws IOException {
    when(targetHttpResponse.getContext()).thenReturn(context);
    when(targetHttpResponse.getBody()).thenReturn(Optional.of(body));
    when(targetHttpResponse.getStatusCode()).thenReturn(200);
    when(context.getResponse()).thenReturn(listenerResponse);
    when(targetHttpResponse.getCallerResponseOutputStream()).thenReturn(responseStream);

    processor.writeTargetResponseOnCaller(targetHttpResponse);

    verify(responseStream).write(responseData.capture());

    assertThat(new String(responseData.getValue()), equalTo(BODY_CONTENT));
  }

  @Test
  void writeTargetResponseOnCaller_removesInvalidAzureRelayHeaders() throws IOException {
    when(targetHttpResponse.getContext()).thenReturn(context);
    when(targetHttpResponse.getBody()).thenReturn(Optional.of(body));
    when(targetHttpResponse.getStatusCode()).thenReturn(200);
    Map<String, String> headers = new HashMap();
    headers.put("transfer-encoding", "chunked");
    when(targetHttpResponse.getHeaders()).thenReturn(Optional.of(headers));
    when(context.getResponse()).thenReturn(listenerResponse);
    when(targetHttpResponse.getCallerResponseOutputStream()).thenReturn(responseStream);

    processor.writeTargetResponseOnCaller(targetHttpResponse);

    assertThat(listenerResponse.getHeaders(), not(hasEntry("transfer-encoding", "chunked")));
  }

  @Test
  void writeTargetResponseOnCaller_withBodyResponseStreamsClose() throws IOException {
    when(targetHttpResponse.getContext()).thenReturn(context);
    when(targetHttpResponse.getBody()).thenReturn(Optional.of(mockBody));
    when(targetHttpResponse.getStatusCode()).thenReturn(200);
    when(context.getResponse()).thenReturn(listenerResponse);
    when(targetHttpResponse.getCallerResponseOutputStream()).thenReturn(responseStream);

    processor.writeTargetResponseOnCaller(targetHttpResponse);

    verify(responseStream).close();
    verify(mockBody).close();
  }

  @Test
  void writeTargetResponseOnCaller_withOutBodyCallerResponseStreamCloses() throws IOException {
    when(targetHttpResponse.getContext()).thenReturn(context);
    when(targetHttpResponse.getBody()).thenReturn(Optional.empty());
    when(targetHttpResponse.getStatusCode()).thenReturn(500);
    when(context.getResponse()).thenReturn(listenerResponse);
    when(targetHttpResponse.getCallerResponseOutputStream()).thenReturn(responseStream);

    processor.writeTargetResponseOnCaller(targetHttpResponse);

    verify(responseStream).close();
  }

  // Note: Unable to figure out how to properly return ResponseStream since it's in a protected
  // package. This is why there's no tests that hit that part of the code.

  @Test
  void writePreflightResponse_Error_noResponse() throws IOException {
    when(context.getRequest()).thenReturn(listenerRequest);
    when(listenerRequest.getHeaders()).thenReturn(requestHeaders);

    Result result = processor.writePreflightResponse(context);

    assertThat("Result is Failure", result.equals(Result.FAILURE));
  }

  @Test
  void writePreflightResponse_Error_InvalidOrigin() throws IOException {
    when(context.getRequest()).thenReturn(listenerRequest);
    when(listenerRequest.getHeaders()).thenReturn(requestHeaders_invalidOrigin);

    Result result = processor.writePreflightResponse(context);

    assertThat("Result is Failure", result.equals(Result.FAILURE));
  }

  private void setUpRelayedHttpRequestMock()
      throws MalformedURLException, URISyntaxException, InvalidRelayTargetException {

    when(targetHostResolver.createTargetUrl(any())).thenReturn(new URL(TARGET_URL));
    when(targetHostResolver.createTargetWebSocketUri(any())).thenReturn(new URI(TARGET_WS_URL));

    when(listenerRequest.getHttpMethod()).thenReturn("POST");
    when(listenerRequest.getInputStream()).thenReturn(body);
    when(listenerRequest.getHeaders()).thenReturn(requestHeaders);
    when(listenerRequest.getUri()).thenReturn(new URI(RELAY_URL));

    when(context.getRequest()).thenReturn(listenerRequest);
  }
}
