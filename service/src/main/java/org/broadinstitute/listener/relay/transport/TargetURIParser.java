package org.broadinstitute.listener.relay.transport;

import java.net.URI;
import java.net.URL;
import java.util.Locale;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.utils.URIBuilder;
import org.broadinstitute.listener.relay.InvalidRelayTargetException;
import org.springframework.web.util.UriUtils;

public class TargetURIParser {
  private static final String WS_HC_SEGMENT = "/$hc";

  private final String targetHost;
  private final URI relayedRequestUri;

  public TargetURIParser(String targetHost, URI relayedRequestUri) {
    this.targetHost = targetHost;
    this.relayedRequestUri = relayedRequestUri;
  }

  public URL parseTargetHttpUrl(String segmentsToRemove) throws InvalidRelayTargetException {

    // remove trailing slash from the host
    String host = StringUtils.stripEnd(targetHost, "/");

    String query = parseQuery();

    try {
      String path = parsePath(segmentsToRemove);
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

  private String parseQuery() {
    String query = "";
    if (StringUtils.isNotBlank(relayedRequestUri.getQuery())) {

      query = "?" + relayedRequestUri.getQuery();
    }
    return query;
  }

  private String parsePath(String segmentsToRemove) {

    // A client establishes a ws connection to Azure Relay to different URL.
    // The URL contains an additional segment: $hc. This segment is not expected in the
    // target, therefore it must be removed.
    String path = removeSegmentsFromPath(WS_HC_SEGMENT, relayedRequestUri.getPath());

    if (StringUtils.isNotBlank(segmentsToRemove)) {
      path = removeSegmentsFromPath(segmentsToRemove, path);
    }
    path = UriUtils.encodePath(path, "UTF-8");

    return path;
  }

  private String removeSegmentsFromPath(String segmentsToRemove, String path) {
    String segments = stripEndAndStartChar(segmentsToRemove, "/");
    String parsedPath = path.replace(segments, "").replace("//", "/");
    parsedPath = stripEndAndStartChar(parsedPath, "/");
    return parsedPath;
  }

  private String stripEndAndStartChar(String segmentsToRemove, String stripChar) {
    String segments = StringUtils.stripStart(segmentsToRemove, stripChar);
    segments = StringUtils.stripEnd(segments, stripChar);
    return segments;
  }
}
