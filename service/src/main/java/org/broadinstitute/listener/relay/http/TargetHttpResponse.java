package org.broadinstitute.listener.relay.http;

import com.microsoft.azure.relay.RelayedHttpListenerContext;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import org.apache.http.client.utils.URIBuilder;

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
      int statusCode, Exception ex, RelayedHttpListenerContext context) {

    String statusDescription = "";
    if (ex != null && ex.getMessage() != null) {
      statusDescription = ex.getMessage();
    }

    return new TargetHttpResponse(null, null, statusCode, statusDescription, context);
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
          .forEach((key, value) -> responseHeaders.put(key, value.iterator().next()));
    }

    InputStream body = (InputStream) clientHttpResponse.body();

    return new TargetHttpResponse(responseHeaders, body, responseStatusCode, "", context);
  }

  private static String createRelayToken(RelayedHttpListenerContext context)
      throws MalformedURLException, URISyntaxException {

    URIBuilder builder = new URIBuilder(context.getListener().getAddress());
    builder.setScheme("https");
    URL url = builder.build().toURL();
    return context
        .getListener()
        .getTokenProvider()
        .getTokenAsync(url.toString(), Duration.ofHours(1))
        .join()
        .getToken();
  }

  public RelayedHttpListenerContext getContext() {
    return context;
  }
}
