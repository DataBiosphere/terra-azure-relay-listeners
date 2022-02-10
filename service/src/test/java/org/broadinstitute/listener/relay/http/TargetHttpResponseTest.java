package org.broadinstitute.listener.relay.http;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.when;

import com.microsoft.azure.relay.RelayedHttpListenerContext;
import java.io.ByteArrayInputStream;
import java.net.http.HttpHeaders;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
  void createTargetHttpResponseFromException_containsExceptionMessageAndStatusCode() {
    when(exception.getMessage()).thenReturn(ERR_MSG);

    targetHttpResponse =
        targetHttpResponse.createTargetHttpResponseFromException(500, exception, context);
    assertThat(targetHttpResponse.getStatusCode(), equalTo(500));
    assertThat(targetHttpResponse.getStatusDescription(), equalTo(ERR_MSG));
  }

  @Test
  void createLocalHttpResponse_containsValidResponse() {
    when(httpResponse.body()).thenReturn(body);
    when(httpHeaders.map()).thenReturn(headers);
    when(httpResponse.headers()).thenReturn(httpHeaders);
    when(httpResponse.statusCode()).thenReturn(200);

    targetHttpResponse = targetHttpResponse.createTargetHttpResponse(httpResponse, context);
    assertThat(targetHttpResponse.getStatusCode(), equalTo(200));
    assertThat(targetHttpResponse.getBody().get(), equalTo(body));
    assertThat(targetHttpResponse.getHeaders().get().keySet(), equalTo(headers.keySet()));
    assertThat(targetHttpResponse.getContext(), equalTo(context));
  }
}
