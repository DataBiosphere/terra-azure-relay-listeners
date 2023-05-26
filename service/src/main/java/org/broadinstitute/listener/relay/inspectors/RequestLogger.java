package org.broadinstitute.listener.relay.inspectors;

import com.microsoft.azure.relay.RelayedHttpListenerRequest;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.TreeMap;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.listener.relay.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RequestLogger {

  private final Logger logger = LoggerFactory.getLogger(RequestLogger.class);
  private static final List<String> MUST_MASKED_HEADER_NAMES = List.of("Authorization", "Cookie");

  /**
   * Logs a relayed HTTP request with the result status code
   *
   * @param relayedHttpListenerRequest Relayed request
   * @param statusCode Result of the request
   * @param requestTimestamp Timestamp of the request
   * @param prefix Logging prefix to include
   * @throws IOException
   * @throws InterruptedException
   */
  public void logRequest(
      RelayedHttpListenerRequest relayedHttpListenerRequest,
      int statusCode,
      OffsetDateTime requestTimestamp,
      String prefix)
      throws IOException, InterruptedException {
    if (relayedHttpListenerRequest == null) {
      logger.warn("Null request provided for logging");
      return;
    }

    // HTTP headers are case-insensitive
    var headers = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
    headers.putAll(relayedHttpListenerRequest.getHeaders());

    var referer = headers.getOrDefault("Referer", "");
    var origin = headers.getOrDefault("Origin", "");
    var ua = headers.getOrDefault("User-Agent", "");

    String endpoint = "";
    if (relayedHttpListenerRequest.getRemoteEndPoint() != null) {
      endpoint = relayedHttpListenerRequest.getRemoteEndPoint().toString();
    }
    var claims = getTokenClaims(headers);

    // log in a single apache-ish line
    logger.info(
        "{} {} {} {} {} '{} {}' {} {} {} '{}' {}",
        prefix,
        statusCode,
        claims.getOrDefault("email", ""),
        claims.getOrDefault("sub", ""),
        claims.getOrDefault("idtyp", ""),
        relayedHttpListenerRequest.getHttpMethod(),
        relayedHttpListenerRequest.getUri(),
        requestTimestamp,
        referer,
        origin,
        ua,
        endpoint);

    logHeaders(relayedHttpListenerRequest.getHeaders());
  }

  private Map<String, String> getTokenClaims(Map<String, String> headers)
      throws IOException, InterruptedException {
    var tokenChecker = new TokenChecker(new GoogleTokenInfoClient());
    var maybeToken = getToken(headers);
    if (maybeToken.isPresent()) {
      return tokenChecker.getOauthInfo(maybeToken.get()).claims();
    }

    return Map.of();
  }

  private Optional<String> getToken(Map<String, String> headers) {
    if (headers == null) {
      logger.error("No auth headers found");
      return Optional.empty();
    }

    var rawToken = Utils.getToken(headers);
    if (rawToken.isEmpty()) {
      logger.error("No valid token found");
      return Optional.empty();
    } else {
      return rawToken;
    }
  }

  private void logHeaders(Map<String, String> headers) {
    if (headers == null) {
      return;
    }

    for (Entry<String, String> header : headers.entrySet()) {
      String value = header.getValue();
      if (isMaskedValue(header.getKey())) {
        value = "*******";
      }
      logger.debug("Header: {} Value:{}", header.getKey(), value);
    }
  }

  private boolean isMaskedValue(String key) {
    for (String maskedHeader : MUST_MASKED_HEADER_NAMES) {
      if (StringUtils.containsIgnoreCase(key, maskedHeader)) {
        return true;
      }
    }

    return false;
  }
}
