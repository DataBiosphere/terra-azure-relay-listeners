package org.broadinstitute.listener.relay.http;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.when;

import com.microsoft.azure.relay.RelayedHttpListenerContext;
import com.microsoft.azure.relay.RelayedHttpListenerRequest;
import com.microsoft.azure.relay.TrackingContext;
import java.io.ByteArrayInputStream;
import java.net.http.HttpHeaders;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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

  @Mock private RelayedHttpListenerRequest relayedHttpListenerRequest;

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

    List<String> validHosts =
        new ArrayList<>() {
          {
            add("*");
          }
        };
    targetHttpResponse =
        targetHttpResponse.createTargetHttpResponseFromException(
            500, exception, context, new CorsSupportProperties("", "", " ", "", validHosts));
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
    when(context.getRequest()).thenReturn(relayedHttpListenerRequest);

    try {
      List<String> validHosts =
          new ArrayList<>() {
            {
              add("*");
            }
          };
      targetHttpResponse =
          targetHttpResponse.createTargetHttpResponse(
              httpResponse, context, new CorsSupportProperties("", "", " ", "", validHosts));
    } catch (Throwable ex) {
    }

    assertThat(targetHttpResponse.getStatusCode(), equalTo(200));
    assertThat(targetHttpResponse.getBody().get(), equalTo(body));

    // check if response headers from the target were included in the listener's response
    for (Entry<String, List<String>> entry : headers.entrySet()) {
      assertThat(
          targetHttpResponse.getHeaders().get(),
          hasEntry(
              entry.getKey(),
              entry.getValue().stream().findFirst().get())); // multi-part headers are not supported
    }

    assertThat(targetHttpResponse.getContext(), equalTo(context));
  }

  @Test
  void createLocalHttpResponse_invalidOrigin() {
    when(httpHeaders.map()).thenReturn(headers);
    when(httpResponse.headers()).thenReturn(httpHeaders);
    when(httpResponse.statusCode()).thenReturn(200);
    when(context.getRequest()).thenReturn(relayedHttpListenerRequest);
    String requestOrigin = "http://malicious.site.com";
    Map<String, String> requestHeaders = Map.of("Origin", requestOrigin);
    when(context.getRequest().getHeaders()).thenReturn(requestHeaders);
    List<String> validHosts = new ArrayList<String>(List.of("app.terra.bio"));

    Throwable thrown = null;
    try {
      targetHttpResponse =
          TargetHttpResponse.createTargetHttpResponse(
              httpResponse, context, new CorsSupportProperties("", "", " ", "", validHosts));
    } catch (Throwable ex) {
      thrown = ex;
      assertThat(
          "Origin exception thrown",
          ex.getMessage().contains(String.format("Origin %s not allowed.", requestOrigin)));
    }
    assertThat(thrown, notNullValue());
  }

  @Test
  void createLocalHttpResponse_validOrigin() {
    when(httpResponse.body()).thenReturn(body);
    when(httpHeaders.map()).thenReturn(headers);
    when(httpResponse.headers()).thenReturn(httpHeaders);
    when(httpResponse.statusCode()).thenReturn(200);
    when(context.getRequest()).thenReturn(relayedHttpListenerRequest);
    String requestOrigin = "http://app.terra.bio";
    Map<String, String> requestHeaders = Map.of("Origin", requestOrigin);
    when(context.getRequest().getHeaders()).thenReturn(requestHeaders);

    List<String> validHosts =
        new ArrayList<>() {
          {
            add("app.terra.bio");
          }
        };

    boolean threw = false;
    try {
      targetHttpResponse =
          TargetHttpResponse.createTargetHttpResponse(
              httpResponse, context, new CorsSupportProperties("", "", " ", "", validHosts));
    } catch (Exception ex) {
      threw = true;
    }
    assertThat(threw, equalTo(false));

    assertThat(targetHttpResponse.getStatusCode(), equalTo(200));
    assertThat(targetHttpResponse.getBody().get(), equalTo(body));

    // check if response headers from the target were included in the listener's response
    for (Entry<String, List<String>> entry : headers.entrySet()) {
      assertThat(
          targetHttpResponse.getHeaders().get(),
          hasEntry(
              entry.getKey(),
              entry.getValue().stream().findFirst().get())); // multi-part headers are not supported
    }

    assertThat(targetHttpResponse.getContext(), equalTo(context));
  }
}
