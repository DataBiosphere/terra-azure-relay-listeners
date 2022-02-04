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
 * and non-functional logic when it's created from the context relayed request.
 */
public class RelayedHttpRequest extends HttpMessage {

  private static final String WS_HC_SEGMENT = "/$hc";

  public URL getTargetURL() {
    return targetURL;
  }

  private final URL targetURL;
  private final String method;
  private final RelayedHttpListenerContext context;

  private RelayedHttpRequest(
      URL targetURL,
      String method,
      Map<String, String> headers,
      InputStream body,
      RelayedHttpListenerContext context) {
    super(headers, body);
    this.targetURL = targetURL;
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

    URL targetURL = createTargetURIFromRelayContext(targetHost, listenerRequest.getUri());

    return new RelayedHttpRequest(
        targetURL, listenerRequest.getHttpMethod(), relayedHeaders, relayedBody, context);
  }

  public static URL createTargetURIFromRelayContext(String targetHost, URI relayedRequestURI)
      throws InvalidRelayTargetException {

    // remove the WSS segment from the URI
    String path = relayedRequestURI.getPath().replace(WS_HC_SEGMENT, "");

    // remove trailing slash from the host and the leading slash for the path
    path = StringUtils.stripStart(path, "/");
    String host = StringUtils.stripEnd(targetHost, "/");

    String query = "";
    if (StringUtils.isNotBlank(relayedRequestURI.getQuery())) {

      query = "?" + relayedRequestURI.getQuery();
    }

    try {
      URI uri = new URI(String.format(Locale.ROOT, "%s/%s%s", host, path, query));

      return uri.toURL();
    } catch (Exception e) {
      throw new InvalidRelayTargetException(
          String.format(
              Locale.ROOT,
              "The target URL could not be parsed. Request URI: %s",
              relayedRequestURI),
          e);
    }
  }

  public String getMethod() {
    return method;
  }

  public RelayedHttpListenerContext getContext() {
    return context;
  }
}
