package org.broadinstitute.listener.relay.http;

import com.microsoft.azure.relay.RelayedHttpListenerContext;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import org.apache.http.client.utils.URIBuilder;

/** Represents an response of the local endpoint that is independent from the HTTP client. */
public class LocalHttpResponse extends HttpMessage {

  public int getStatusCode() {
    return statusCode;
  }

  public String getStatusDescription() {
    return statusDescription;
  }

  private final int statusCode;
  private final RelayedHttpListenerContext context;
  private final String statusDescription;

  private LocalHttpResponse(
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

  public static LocalHttpResponse createErrorLocalHttpResponse(
      int statusCode, Exception ex, RelayedHttpListenerContext context) {

    String statusDescription = "";
    if (ex != null && ex.getMessage() != null) {
      statusDescription = ex.getMessage();
    }

    return new LocalHttpResponse(null, null, statusCode, statusDescription, context);
  }

  public static LocalHttpResponse createLocalHttpResponse(
      HttpResponse<?> clientHttpResponse, RelayedHttpListenerContext context)
      throws MalformedURLException, URISyntaxException {
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

    responseHeaders.put("ServiceBusAuthorization", createRelayToken(context));

    InputStream body = (InputStream) clientHttpResponse.body();

    return new LocalHttpResponse(responseHeaders, body, responseStatusCode, "", context);
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
