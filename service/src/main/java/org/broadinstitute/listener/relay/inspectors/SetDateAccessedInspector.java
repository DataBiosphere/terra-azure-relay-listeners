package org.broadinstitute.listener.relay.inspectors;

import com.google.common.base.Strings;
import com.microsoft.azure.relay.RelayedHttpListenerRequest;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.Locale;
import org.apache.http.client.utils.URIBuilder;
import org.broadinstitute.listener.relay.Utils;
import org.broadinstitute.listener.relay.inspectors.InspectorType.InspectorNameConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component(InspectorNameConstants.SET_DATE_ACCESSED)
public class SetDateAccessedInspector implements RequestInspector {
  private final Logger logger = LoggerFactory.getLogger(SetDateAccessedInspector.class);
  private Instant lastAccessedDate;
  private final URL serviceUrl;

  private final int requestWindowInSeconds;
  private final HttpClient httpClient;
  private static final String API_SEGMENT_PATTERN =
      "%s/api/v2/runtimes/%s/<runtimeName>/setDateAccessed";

  public SetDateAccessedInspector(
      int requestWindowInSeconds, String serviceHost, String workspaceId, HttpClient httpClient)
      throws URISyntaxException, MalformedURLException {

    if (requestWindowInSeconds <= 0) {
      throw new IllegalArgumentException(
          "The request value in seconds is invalid. It must be greater than 0");
    }

    if (Strings.isNullOrEmpty(serviceHost)) {
      throw new IllegalArgumentException(
          "The service host is missing. Please check the configuration");
    }

    if (Strings.isNullOrEmpty(workspaceId)) {
      throw new IllegalArgumentException(
          "The workspace id is missing. Please check the configuration");
    }

    this.httpClient = httpClient;
    this.requestWindowInSeconds = requestWindowInSeconds;

    lastAccessedDate = Instant.now().plusSeconds(requestWindowInSeconds);
    URIBuilder builder =
        new URIBuilder(String.format(Locale.ROOT, API_SEGMENT_PATTERN, serviceHost, workspaceId));

    serviceUrl = builder.build().toURL();
  }

  @Override
  public boolean inspectWebSocketUpgradeRequest(
      RelayedHttpListenerRequest relayedHttpListenerRequest) {

    return checkLastAccessDateAndCallServiceIfExpired(relayedHttpListenerRequest);
  }

  @Override
  public boolean inspectRelayedHttpRequest(RelayedHttpListenerRequest relayedHttpListenerRequest) {

    return checkLastAccessDateAndCallServiceIfExpired(relayedHttpListenerRequest);
  }

  private boolean checkLastAccessDateAndCallServiceIfExpired(RelayedHttpListenerRequest relayedHttpListenerRequest) {
    if (hasLastAccessDateExpired()){
      setLastAccessedDateOnService(relayedHttpListenerRequest);
      updateLastAccessedDate();
    }
    return true;
  }

  public void setLastAccessedDateOnService(RelayedHttpListenerRequest relayedHttpListenerRequest) {
    HttpRequest request;
    try {
      request =
          HttpRequest.newBuilder()
              .uri(serviceUrl.toURI())
              .method("PATCH", HttpRequest.BodyPublishers.noBody())
              .header(
                  "Authorization",
                  "Bearer "
                      + Utils.getTokenFromAuthorization(relayedHttpListenerRequest.getHeaders()))
              .build();
    } catch (URISyntaxException e) {
      logger.error("Failed to parse the URL to set the date accessed via the API", e);
      throw new RuntimeException(e);
    }

    logger.debug("Making a call to the last accessed date API at this URL: {}", serviceUrl);

    HttpResponse<String> response;
    try {
      response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    } catch (IOException | InterruptedException e) {
      logger.error("Failed to call the set the date accessed API", e);
      throw new RuntimeException(e);
    }

    logger.info(
        "The set last accessed date API call was made with the following response: {}",
        response.statusCode());
  }

  private synchronized boolean hasLastAccessDateExpired() {
    return Instant.now().isBefore(lastAccessedDate);
  }

  private synchronized void updateLastAccessedDate() {
    lastAccessedDate = lastAccessedDate.plusSeconds(requestWindowInSeconds);
  }
}
