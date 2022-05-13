package org.broadinstitute.listener.relay.transport;

import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.utils.URIBuilder;
import org.broadinstitute.listener.config.ListenerProperties;
import org.broadinstitute.listener.relay.InvalidRelayTargetException;
import org.springframework.lang.NonNull;
import org.springframework.web.util.UriUtils;

public class DefaultTargetResolver implements TargetResolver {
  private static final String WS_HC_SEGMENT = "/$hc";

  private final String targetHost;
  private final ListenerProperties properties;

  public DefaultTargetResolver(ListenerProperties properties) {
    this.properties = properties;
    if (properties.getTargetProperties() == null
        || StringUtils.isBlank(properties.getTargetProperties().getTargetHost())) {
      throw new IllegalStateException("The target host configuration is missing.");
    }
    targetHost = properties.getTargetProperties().getTargetHost();
  }

  @Override
  public String resolveTargetHost() {

    return targetHost;
  }

  @Override
  public URI createTargetWebSocketUri(@NonNull URI relayedRequestUri)
      throws InvalidRelayTargetException {
    URL targetUrl =
        createTargetUrl(
            relayedRequestUri, properties.getTargetProperties().isRemoveEntityPathFromWssUri());

    if (targetUrl.getProtocol().equals("http")) {
      return URI.create(targetUrl.toString().replaceFirst("http://", "ws://"));
    }

    if (targetUrl.getProtocol().equals("https")) {
      return URI.create(targetUrl.toString().replaceFirst("https://", "wss://"));
    }

    throw new InvalidRelayTargetException(
        "Invalid target URL. The target must be an HTTP/HTTPS endpoint");
  }

  @Override
  public URL createTargetUrl(@NonNull URI relayedRequestUri) throws InvalidRelayTargetException {

    return createTargetUrl(
        relayedRequestUri, properties.getTargetProperties().isRemoveEntityPathFromHttpUrl());
  }

  private URL createTargetUrl(URI relayedRequestUri, boolean removeEntityPath)
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
      path = UriUtils.encodePath(path, "UTF-8");
      if (removeEntityPath) {
        path = Arrays.stream(path.split("/")).skip(1).collect(Collectors.joining("/"));
      }
      URIBuilder builder = new URIBuilder(String.format(Locale.ROOT, "%s/%s%s", host, path, query));

      return builder.build().toURL();
    } catch (Exception e) {
      throw new InvalidRelayTargetException(
          String.format(
              Locale.ROOT,
              "The target URL could not be parsed. Request URI: %s",
              relayedRequestUri),
          e);
    }
  }
}
