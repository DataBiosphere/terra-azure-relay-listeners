package org.broadinstitute.listener.relay.http;

import com.microsoft.azure.relay.RelayedHttpListenerContext;
import com.microsoft.azure.relay.RelayedHttpListenerRequest;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import org.broadinstitute.listener.relay.InvalidRelayTargetException;
import org.broadinstitute.listener.relay.transport.TargetResolver;
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

  public URI getTargetWebSocketUri() {
    return targetWebSocketUri;
  }

  private final URI targetWebSocketUri;

  private RelayedHttpRequest(
      URL targetUrl,
      String method,
      Map<String, String> headers,
      InputStream body,
      RelayedHttpListenerContext context,
      URI targetWebSocketUri) {
    super(headers, body);
    this.targetUrl = targetUrl;
    this.method = method;
    this.context = context;
    this.targetWebSocketUri = targetWebSocketUri;
  }

  public static RelayedHttpRequest createRelayedHttpRequest(
      @NonNull RelayedHttpListenerContext context, @NonNull TargetResolver targetResolver)
      throws InvalidRelayTargetException {

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

    URL targetUrl = targetResolver.createTargetUrl(listenerRequest.getUri());
    URI targetWebSocketUri = targetResolver.createTargetWebSocketUri(listenerRequest.getUri());

    return new RelayedHttpRequest(
        targetUrl,
        listenerRequest.getHttpMethod(),
        relayedHeaders,
        relayedBody,
        context,
        targetWebSocketUri);
  }

  public String getMethod() {
    return method;
  }

  public RelayedHttpListenerContext getContext() {
    return context;
  }
}
