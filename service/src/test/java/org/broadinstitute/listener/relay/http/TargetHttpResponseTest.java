package org.broadinstitute.listener.relay.http;

import static com.google.common.net.HttpHeaders.CONTENT_SECURITY_POLICY;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.when;

import com.microsoft.azure.relay.RelayedHttpListenerContext;
import com.microsoft.azure.relay.TrackingContext;
import java.io.ByteArrayInputStream;
import java.net.http.HttpHeaders;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.broadinstitute.listener.config.CorsSupportProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TargetHttpResponseTest {

  private TargetHttpResponse targetHttpResponse;

  private final String ERR_MSG = "Error Message";

  @Mock private Exception exception;

  @Mock private RelayedHttpListenerContext context;

  @Mock private TrackingContext trackingContext;

  @Mock private ByteArrayInputStream body;

  @Mock private HttpResponse httpResponse;

  @Mock private HttpHeaders httpHeaders;

  private Map<String, List<String>> headers;

  @BeforeEach
  void setUp() {
    headers = new HashMap<>();
    headers.put("HEAD1", List.of("VALUE1"));
    headers.put("HEAD2", List.of("VALUE2"));
  }

  @Test
  void createTargetHttpResponseFromException_containsExceptionMessageStatusCodeAndBody() {
    when(exception.getMessage()).thenReturn(ERR_MSG);
    when(context.getTrackingContext()).thenReturn(trackingContext);
    when(trackingContext.getTrackingId()).thenReturn("123abc");

    targetHttpResponse =
        targetHttpResponse.createTargetHttpResponseFromException(
            500, exception, context, new CorsSupportProperties("", "", " ", ""));
    assertThat(targetHttpResponse.getStatusCode(), equalTo(500));
    assertThat(targetHttpResponse.getStatusDescription(), equalTo(ERR_MSG));
    assertThat(targetHttpResponse.getBody().isPresent(), equalTo(true));
  }

  @Test
  void createLocalHttpResponse_containsValidResponse() {
    when(httpResponse.body()).thenReturn(body);
    when(httpHeaders.map()).thenReturn(headers);
    when(httpResponse.headers()).thenReturn(httpHeaders);
    when(httpResponse.statusCode()).thenReturn(200);

    targetHttpResponse =
        targetHttpResponse.createTargetHttpResponse(
            httpResponse, context, new CorsSupportProperties("", "", " ", ""));
    assertThat(targetHttpResponse.getStatusCode(), equalTo(200));
    assertThat(targetHttpResponse.getBody().get(), equalTo(body));
    headers.put(CONTENT_SECURITY_POLICY, List.of("dummy"));
    assertThat(targetHttpResponse.getHeaders().get().keySet(), equalTo(headers.keySet()));
    assertThat(targetHttpResponse.getContext(), equalTo(context));
  }
}
