package org.broadinstitute.listener.relay.http;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.microsoft.azure.relay.RelayedHttpListenerContext;
import com.microsoft.azure.relay.RelayedHttpListenerResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
  private InputStream body;
  private RelayedHttpRequestProcessor processor;

  @Mock private HttpClient httpClient;

  @Mock private RelayedHttpListenerContext context;
  @Mock private RelayedHttpRequest request;
  @Mock private HttpResponse targetClientResponse;
  @Mock private HttpHeaders targetResponseHttpHeaders;
  @Mock private RelayedHttpListenerResponse listenerResponse;
  @Mock private OutputStream responseStream;
  @Mock private TargetHttpResponse targetHttpResponse;

  @Captor private ArgumentCaptor<byte[]> responseData;

  private Map<String, List<String>> targetResponseHeaders;
  private Map<String, String> requestHeaders;

  @BeforeEach
  void setUp() throws IOException, InterruptedException {
    body = new ByteArrayInputStream(BODY_CONTENT.getBytes(StandardCharsets.UTF_8));
    targetResponseHeaders = new HashMap<>();
    targetResponseHeaders.put("RES_HEADER", List.of("RES_VALUE"));
    requestHeaders = new HashMap<>();
    requestHeaders.put("REQ_HEADER", "REQ_VALUE");
    processor = new RelayedHttpRequestProcessor(httpClient);
  }

  @Test
  void executeRequestOnTarget_successfullyExecuted() throws IOException, InterruptedException {
    setUpRelayedHttpRequestMock();
    when(targetClientResponse.statusCode()).thenReturn(200);
    when(httpClient.send(any(), any())).thenReturn(targetClientResponse);
    when(targetClientResponse.headers()).thenReturn(targetResponseHttpHeaders);
    when(targetResponseHttpHeaders.map()).thenReturn(targetResponseHeaders);
    when(targetClientResponse.body()).thenReturn(body);

    TargetHttpResponse response = processor.executeRequestOnTarget(request);
    String data = new String(response.getBody().get().readAllBytes());

    assertThat(response.getStatusCode(), equalTo(200));
    assertThat(response.getHeaders().get().keySet(), equalTo(targetResponseHeaders.keySet()));
    assertThat(data, equalTo(BODY_CONTENT));
  }

  @Test
  void executeRequestOnTarget_failedToExecute() throws IOException, InterruptedException {
    setUpRelayedHttpRequestMock();
    when(httpClient.send(any(), any())).thenThrow(new InterruptedException("Error Msg"));
    TargetHttpResponse response = processor.executeRequestOnTarget(request);

    assertThat(response.getStatusCode(), equalTo(500));
    assertThat(response.getStatusDescription(), equalTo("Error Msg"));
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

  private void setUpRelayedHttpRequestMock() throws MalformedURLException {
    when(request.getTargetUrl()).thenReturn(new URL(TARGET_URL));
    when(request.getMethod()).thenReturn("POST");
    when(request.getBody()).thenReturn(Optional.of(body));
    when(request.getHeaders()).thenReturn(Optional.of(requestHeaders));
    when(request.getContext()).thenReturn(context);
  }
}
