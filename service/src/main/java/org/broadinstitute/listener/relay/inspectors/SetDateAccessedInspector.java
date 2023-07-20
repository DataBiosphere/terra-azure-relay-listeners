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
import java.util.Map;
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

  private final int callWindowInSeconds;
  private final HttpClient httpClient;
  private final String leonardoServiceAccountEmail;
  private final TokenChecker tokenChecker;
  private static final int MIN_CALL_WINDOW_IN_SECONDS = 1;
  private static final String API_ENDPOINT_PATTERN = "%s/api/v2/runtimes/%s/%s/updateDateAccessed";

  public SetDateAccessedInspector(SetDateAccessedInspectorOptions options)
      throws URISyntaxException, MalformedURLException {

    validateOptions(options);

    this.httpClient = options.httpClient();
    this.callWindowInSeconds = options.callWindowInSeconds();
    this.leonardoServiceAccountEmail = options.leonardoServiceAccountEmail();
    this.tokenChecker = options.tokenChecker();

    lastAccessedDate = Instant.now();
    URIBuilder builder =
        new URIBuilder(
            String.format(
                Locale.ROOT,
                API_ENDPOINT_PATTERN,
                options.serviceHost(),
                options.workspaceId(),
                options.runtimeName()));

    serviceUrl = builder.build().toURL();
  }

  private void validateOptions(SetDateAccessedInspectorOptions options) {
    if (options == null) {
      throw new IllegalArgumentException("Inspector options can't be null");
    }

    if (options.callWindowInSeconds() <= MIN_CALL_WINDOW_IN_SECONDS) {
      throw new IllegalArgumentException(
          "The call window value in seconds is invalid. It must be greater than "
              + MIN_CALL_WINDOW_IN_SECONDS);
    }

    if (Strings.isNullOrEmpty(options.leonardoServiceAccountEmail())) {
      throw new IllegalArgumentException(
          "The service account email is missing. Please check the configuration");
    }

    if (Strings.isNullOrEmpty(options.serviceHost())) {
      throw new IllegalArgumentException(
          "The service host is missing. Please check the configuration");
    }

    if (Strings.isNullOrEmpty(options.runtimeName())) {
      throw new IllegalArgumentException(
          "The runtime name is missing. Please check the configuration");
    }

    if (options.workspaceId() == null) {
      throw new IllegalArgumentException(
          "The workspace id is missing. Please check the configuration");
    }

    if (options.httpClient() == null) {
      throw new IllegalArgumentException("The http client can't be null");
    }

    if (options.tokenChecker() == null) {
      throw new IllegalArgumentException("The token checker can't be null");
    }
  }

  @Override
  public boolean inspectWebSocketUpgradeRequest(
      RelayedHttpListenerRequest relayedHttpListenerRequest) {

    return checkLastAccessDateAndCallServiceIfExpired(relayedHttpListenerRequest);
  }

  /**
   * Inspect the request, calling Leonardo to updateDateAccessed on our resource if the request
   * represents an action from a user other than the Leonardo service account.
   */
  @Override
  public boolean inspectRelayedHttpRequest(RelayedHttpListenerRequest relayedHttpListenerRequest) {
    logger.warn("DATE ACCESSED LISTENER !!! {}", relayedHttpListenerRequest.getUri());
    logger.warn(relayedHttpListenerRequest.getHeaders().toString());
    if (isLeonardoServiceAccountUser(relayedHttpListenerRequest)) {
      logger.info(
          "Not setting date accessed for a service account request to {}",
          relayedHttpListenerRequest.getUri().toString());
      return true;
    } else {
      return checkLastAccessDateAndCallServiceIfExpired(relayedHttpListenerRequest);
    }
  }

  private boolean isLeonardoServiceAccountUser(
      RelayedHttpListenerRequest relayedHttpListenerRequest) {
    Map<String, String> headers = relayedHttpListenerRequest.getHeaders();
    if (headers == null) {
      logger.error("No auth headers found");
      return false;
    }

    var leoToken = Utils.getToken(headers);

    if (leoToken.isEmpty()) {
      logger.error("No valid token found");
      return false;
    } else {
      try {
        var token = leoToken.get();
        return tokenChecker.isTokenForUser(token, leonardoServiceAccountEmail);
      } catch (IOException | InterruptedException e) {
        logger.error("Failed to check token info", e);
        return false;
      } catch (Exception e) {
        logger.error("Failed for unknown reasons", e);
        return false;
      }
    }
  }

  private boolean checkLastAccessDateAndCallServiceIfExpired(
      RelayedHttpListenerRequest relayedHttpListenerRequest) {
    try {
      if (hasLastAccessDateExpired()) {
        setLastAccessedDateOnService(relayedHttpListenerRequest);
        updateLastAccessedDate();
      }
    } catch (RuntimeException ex) {
      logger.error(
          "Failed to set the last accessed date. The request still will get processed", ex);
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
                      + Utils.getToken(relayedHttpListenerRequest.getHeaders())
                          .orElseThrow(
                              () ->
                                  new RuntimeException(
                                      "Authorization token not found in the request")))
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
      logger.error("Failed to call the set date accessed API", e);
      throw new RuntimeException(e);
    }

    logger.info(
        "The set last accessed date API call was made with the following response: {}",
        response.statusCode());
  }

  private synchronized boolean hasLastAccessDateExpired() {
    return Instant.now().isAfter(lastAccessedDate);
  }

  private synchronized void updateLastAccessedDate() {
    lastAccessedDate = lastAccessedDate.plusSeconds(callWindowInSeconds);
  }
}
