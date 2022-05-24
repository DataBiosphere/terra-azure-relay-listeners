package org.broadinstitute.listener.relay.http;

import com.microsoft.azure.relay.RelayedHttpListenerContext;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static com.google.common.net.HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS;
import static com.google.common.net.HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS;
import static com.google.common.net.HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN;
import static com.google.common.net.HttpHeaders.ACCESS_CONTROL_MAX_AGE;
import static com.google.common.net.HttpHeaders.CONTENT_SECURITY_POLICY;

/**
 * Represents a response of the local endpoint that is independent of the HTTP client
 * implementation.
 */
public class TargetHttpResponse extends HttpMessage {
  public int getStatusCode() {
    return statusCode;
  }

  public String getStatusDescription() {
    return statusDescription;
  }

  private final int statusCode;
  private final RelayedHttpListenerContext context;
  private final String statusDescription;

  private TargetHttpResponse(
      Map<String, String> headers,
      InputStream body,
      int statusCode,
      String statusDescription,
      RelayedHttpListenerContext context) {
    super(headers, body);
    this.statusCode = statusCode;
    this.statusDescription = statusDescription;
    this.context = context;
  }

  public OutputStream getCallerResponseOutputStream() {
    return (OutputStream) context.getResponse().getOutputStream();
  }

  public static TargetHttpResponse createTargetHttpResponseFromException(
      int statusCode, Throwable ex, RelayedHttpListenerContext context) {

    String statusDescription = "";
    if (ex != null && ex.getMessage() != null) {
      statusDescription = ex.getMessage();
    }

    String body =
        String.format(
            "{ \"message\":\"The listener failed to process the request.\", \"tracking_id\":\"%s\"}",
            context.getTrackingContext().getTrackingId());

    Map<String, String> headers = new HashMap<>();
    headers.put("Content-Type", "application/json");

    return new TargetHttpResponse(
        headers,
        new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8)),
        statusCode,
        statusDescription,
        context);
  }

  public static TargetHttpResponse createTargetHttpResponse(
      HttpResponse<?> clientHttpResponse, RelayedHttpListenerContext context) {
    int responseStatusCode = clientHttpResponse.statusCode();
    Map<String, String> responseHeaders = new HashMap<>();
    if (clientHttpResponse.headers() != null && !clientHttpResponse.headers().map().isEmpty()) {

      // TODO: implement multi header value support.
      // only the first header value is set for now.
      clientHttpResponse
          .headers()
          .map()
          .forEach(
              (key, value) -> {
                String headerValue = value.iterator().next();
                if (headerValue != ACCESS_CONTROL_ALLOW_ORIGIN && headerValue != CONTENT_SECURITY_POLICY)
                  responseHeaders.put(key, headerValue);
              });

      responseHeaders.put(ACCESS_CONTROL_ALLOW_ORIGIN, "*");
      responseHeaders.put(ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
      responseHeaders.put(ACCESS_CONTROL_ALLOW_HEADERS, "Authorization, Content-Type, Accept, Origin, X-App-Id");
      responseHeaders.put(ACCESS_CONTROL_MAX_AGE, "1728000");
//      responseHeaders.put(CONTENT_SECURITY_POLICY, "*"); TODO: add CSP in the future
    }

    InputStream body = (InputStream) clientHttpResponse.body();

    return new TargetHttpResponse(responseHeaders, body, responseStatusCode, "", context);
  }

  public RelayedHttpListenerContext getContext() {
    return context;
  }
}
