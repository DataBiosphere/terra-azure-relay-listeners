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

  private final int callWindowInSeconds;
  private final HttpClient httpClient;
  private static final int MIN_CALL_WINDOW_IN_SECONDS = 1;
  private static final String API_ENDPOINT_PATTERN = "%s/api/v2/runtimes/%s/%s/updateDateAccessed";

  public SetDateAccessedInspector(SetDateAccessedInspectorOptions options)
      throws URISyntaxException, MalformedURLException {

    validateOptions(options);

    this.httpClient = options.httpClient();
    this.callWindowInSeconds = options.callWindowInSeconds();

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
    logger.info("setLastAccessedDateOnService\n\tRelayed request: "
      .concat(relayedHttpListenerRequestToString(relayedHttpListenerRequest))
    );

    HttpRequest request;
    try {
      request =
          HttpRequest.newBuilder()
              .uri(serviceUrl.toURI())
              .method("PATCH", HttpRequest.BodyPublishers.noBody())
              .header(
                  "Authorization",
                  "Bearer "
                      + Utils.getTokenFromAuthorization(relayedHttpListenerRequest.getHeaders())
                          .orElseThrow(
                              () ->
                                  new RuntimeException(
                                      "Authorization token not found in the request")))
              .build();
    } catch (URISyntaxException e) {
      logger.error("Failed to parse the URL to set the date accessed via the API", e);
      throw new RuntimeException(e);
    }

    logger.info("setLastAccessedDateOnService\n\tMaking a call to the last accessed date API at this URL: "
      .concat(serviceUrl.toString())
      .concat("\n\tRequest to Leo: ")
      .concat(httpRequestToString(request))
    );

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

  private String httpRequestToString(HttpRequest request) {
    return "method: "
      .concat(request.method())
      .concat("\n\turi: ")
      .concat(request.uri().toString())
      .concat("\n\theaders: ")
      .concat(request.headers().map().toString());
  }

  private String relayedHttpListenerRequestToString(RelayedHttpListenerRequest request) {
    return "method: "
      .concat(request.getHttpMethod())
      .concat("uri: ")
      .concat(request.getUri().toString())
      .concat("headers: ")
      .concat(request.getHeaders().toString());
  }
}
