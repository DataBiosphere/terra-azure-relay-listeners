package org.broadinstitute.listener.relay.http;

import com.microsoft.azure.relay.RelayedHttpListenerContext;
import com.microsoft.azure.relay.RelayedHttpListenerRequest;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.listener.relay.InvalidRelayTargetException;
import org.springframework.lang.NonNull;

/**
 * This class represents the relayed request from the client and includes the information of the
 * local target URI.
 *
 * <p>Its main purpose is to provide an abstraction to facilitate the implementation of functional
 * and non-functional logic when created from the context relayed request.
 */
public class RelayedHttpRequest extends HttpMessage {

  private static final String WS_HC_SEGMENT = "/$hc";

  public URL getTargetUrl() {
    return targetUrl;
  }

  private final URL targetUrl;
  private final String method;
  private final RelayedHttpListenerContext context;

  private RelayedHttpRequest(
      URL targetUrl,
      String method,
      Map<String, String> headers,
      InputStream body,
      RelayedHttpListenerContext context) {
    super(headers, body);
    this.targetUrl = targetUrl;
    this.method = method;
    this.context = context;
  }

  public static RelayedHttpRequest createRelayedHttpRequest(
      @NonNull RelayedHttpListenerContext context, String targetHost)
      throws InvalidRelayTargetException {
    if (StringUtils.isBlank(targetHost)) {
      throw new IllegalArgumentException("The target host is blank or null.");
    }

    RelayedHttpListenerRequest listenerRequest = context.getRequest();

    Map<String, String> relayedHeaders = null;

    if (listenerRequest.getHeaders() != null) {
      relayedHeaders = new HashMap<>(listenerRequest.getHeaders());
    }

    InputStream relayedBody = null;

    if (!listenerRequest.getHttpMethod().equals("GET")
        && !listenerRequest.getHttpMethod().equals("HEAD")) {
      relayedBody = listenerRequest.getInputStream();
    }

    URL targetURL = createTargetUriFromRelayContext(targetHost, listenerRequest.getUri());

    return new RelayedHttpRequest(
        targetURL, listenerRequest.getHttpMethod(), relayedHeaders, relayedBody, context);
  }

  private static URL createTargetUriFromRelayContext(String targetHost, URI relayedRequestUri)
      throws InvalidRelayTargetException {

    // remove the WSS segment from the URI
    String path = relayedRequestUri.getPath().replace(WS_HC_SEGMENT, "");

    // remove trailing slash from the host and the leading slash for the path
    path = StringUtils.stripStart(path, "/");
    String host = StringUtils.stripEnd(targetHost, "/");

    String query = "";
    if (StringUtils.isNotBlank(relayedRequestUri.getQuery())) {

      query = "?" + relayedRequestUri.getQuery();
    }

    try {
      URI uri = new URI(String.format(Locale.ROOT, "%s/%s%s", host, path, query));

      return uri.toURL();
    } catch (Exception e) {
      throw new InvalidRelayTargetException(
          String.format(
              Locale.ROOT,
              "The target URL could not be parsed. Request URI: %s",
              relayedRequestUri),
          e);
    }
  }

  public URI getWebSocketTargetUri() {
    if (targetUrl.getProtocol().equals("http")) {
      return URI.create(targetUrl.toString().replaceFirst("http://", "ws://"));
    }

    if (targetUrl.getProtocol().equals("https")) {
      return URI.create(targetUrl.toString().replaceFirst("https://", "wss://"));
    }

    throw new RuntimeException("Invalid target URL. The target must be an HTTP/HTTPS endpoint");
  }

  public String getMethod() {
    return method;
  }

  public RelayedHttpListenerContext getContext() {
    return context;
  }
}
