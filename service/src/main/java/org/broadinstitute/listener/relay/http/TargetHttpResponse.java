package org.broadinstitute.listener.relay.http;

import static com.google.common.net.HttpHeaders.CONTENT_SECURITY_POLICY;

import com.microsoft.azure.relay.RelayedHttpListenerContext;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.broadinstitute.listener.config.CorsSupportProperties;

/**
 * Represents a response of the local endpoint that is independent of the HTTP client
 * implementation.
 */
public class TargetHttpResponse extends HttpMessage {
  private final CorsSupportProperties corsSupportProperties;

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
      CorsSupportProperties corsSupportProperties,
      int statusCode,
      String statusDescription,
      RelayedHttpListenerContext context) {
    super(headers, body);
    this.corsSupportProperties = corsSupportProperties;
    this.statusCode = statusCode;
    this.statusDescription = statusDescription;
    this.context = context;
  }

  public OutputStream getCallerResponseOutputStream() {
    return (OutputStream) context.getResponse().getOutputStream();
  }

  public static TargetHttpResponse createTargetHttpResponseFromException(
      int statusCode,
      Throwable ex,
      RelayedHttpListenerContext context,
      CorsSupportProperties corsSupportProperties) {

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
        corsSupportProperties,
        statusCode,
        statusDescription,
        context);
  }

  public static TargetHttpResponse createTargetHttpResponse(
      HttpResponse<?> clientHttpResponse,
      RelayedHttpListenerContext context,
      CorsSupportProperties corsSupportProperties) {
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
                if (!key.equalsIgnoreCase(CONTENT_SECURITY_POLICY)) {
                  responseHeaders.put(key, headerValue);
                }
              });

      responseHeaders.put(CONTENT_SECURITY_POLICY, corsSupportProperties.contentSecurityPolicy());
    }

    InputStream body = (InputStream) clientHttpResponse.body();

    return new TargetHttpResponse(
        responseHeaders, body, corsSupportProperties, responseStatusCode, "", context);
  }

  public RelayedHttpListenerContext getContext() {
    return context;
  }
}
