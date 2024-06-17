package org.broadinstitute.listener.relay.http;

import static com.google.common.net.HttpHeaders.CONTENT_SECURITY_POLICY;
import static com.google.common.net.HttpHeaders.SET_COOKIE;

import com.microsoft.azure.relay.RelayedHttpListenerContext;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.broadinstitute.listener.config.CorsSupportProperties;
import org.broadinstitute.listener.relay.Utils;

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
      CorsSupportProperties corsSupportProperties)
      throws Exception {
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
                  if (key.equalsIgnoreCase(SET_COOKIE)) {
                    // setcookie response from jupyter lab looks like this: set-cookie:
                    // _xsrf=2|63084c74|2d3173085f60f5a3889e8c1e1879d0a6|1654868473; expires=Sun, 10
                    // Jul 2022 13:41:13 GMT; Path=/saturn-403635c5-c58b-4bcd-b3d1-55aa5bd8919d/
                    var cookieValue =
                        String.format(
                            "%s; Secure; SameSite=None; HttpOnly; Partitioned", headerValue);
                    responseHeaders.put(key, cookieValue);
                  } else responseHeaders.put(key, headerValue);
                }
              });
      Map<String, String> requestHeaders = context.getRequest().getHeaders();
      if (Utils.isValidOrigin(requestHeaders.getOrDefault("Origin", ""), corsSupportProperties)) {
        Utils.writeCORSHeaders(responseHeaders, requestHeaders, corsSupportProperties);
      } else {
        throw new Exception(
            String.format(
                "Origin %s not allowed. Error Code: RHRP-003",
                requestHeaders.getOrDefault("Origin", "")));
      }
    }

    InputStream body = (InputStream) clientHttpResponse.body();

    return new TargetHttpResponse(
        responseHeaders, body, corsSupportProperties, responseStatusCode, "", context);
  }

  public RelayedHttpListenerContext getContext() {
    return context;
  }
}
