package org.broadinstitute.listener.relay;

import com.microsoft.azure.relay.RelayedHttpListenerRequest;
import org.broadinstitute.listener.config.CorsSupportProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import static com.google.common.net.HttpHeaders.*;

public class Utils {

  public static final Logger logger = LoggerFactory.getLogger(Utils.class);
  public static final String TOKEN_NAME = "LeoToken";
  public static final String SET_COOKIE_API_PATH = "setcookie";
  public static final List<String> GET_STATUS_API_PATH = List.of("api", "status");
  public static final List<String> GET_WELDER_STATUS_API_PATH = List.of("welder", "status");

  public static final Optional<String> getTokenFromAuthorization(Map<String, String> headers) {
    var authValue = headers.getOrDefault(AUTHORIZATION, null);

    return Optional.ofNullable(authValue)
        .filter(s -> s.contains("Bearer "))
        .map(s -> s.replaceFirst("Bearer ", "").trim());
  }

  public static boolean isSetCookiePath(URI uri) {
    var splitted = uri.getPath().split("/");
    if (splitted.length == 3) {
      return splitted[2].toLowerCase().equals(SET_COOKIE_API_PATH);
    } else {
      return false;
    }
  }

  public static boolean isNotGetStatusRequest(RelayedHttpListenerRequest listenerRequest) {
    var isGetStatusRequest = listenerRequest.getHttpMethod().equals("GET")
        && isGetStatusPath(listenerRequest.getUri());
    return !isGetStatusRequest;
  }

  private static boolean isGetStatusPath(URI uri) {
    var splitted = Arrays.asList(uri.getPath().split("/"));
    if (splitted.size() == 4) {
      var tail = splitted.subList(splitted.size() - 2, splitted.size() - 1);
      return GET_STATUS_API_PATH.equals(tail) || GET_WELDER_STATUS_API_PATH.equals(tail);
    } else {
      return false;
    }
  }

  public static void writeCORSHeaders(
      Map<String, String> responseHeaders,
      Map<String, String> requestHeaders,
      CorsSupportProperties corsSupportProperties) {
    responseHeaders.putIfAbsent(
        ACCESS_CONTROL_ALLOW_METHODS, corsSupportProperties.preflightMethods());

    responseHeaders.putIfAbsent(
        ACCESS_CONTROL_ALLOW_ORIGIN, requestHeaders.getOrDefault("Origin", "*"));

    responseHeaders.putIfAbsent(ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
    responseHeaders.putIfAbsent(
        CONTENT_SECURITY_POLICY, corsSupportProperties.contentSecurityPolicy());
    responseHeaders.putIfAbsent(ACCESS_CONTROL_ALLOW_HEADERS, corsSupportProperties.allowHeaders());
    responseHeaders.putIfAbsent(ACCESS_CONTROL_MAX_AGE, corsSupportProperties.maxAge());
  }

  public static boolean isValidOrigin(String origin, CorsSupportProperties corsSupportProperties) {
    String stringToCheck;
    // We want to strip the protocol.
    try {
      URL url = new URL(origin);
      stringToCheck = url.getHost();
    } catch (MalformedURLException e) {
      stringToCheck = origin;
    }

    logger.info(String.format("Checking origin %s", stringToCheck));
    logger.info(corsSupportProperties.validHosts().toString());
    // To appeal to lambda.
    String finalStringToCheck = stringToCheck;
    return origin.isEmpty()
        || corsSupportProperties.validHosts().stream()
            .anyMatch(
                validHost -> Pattern.matches(
                    validHost.replace(".", "\\.").replace("*", ".*").replace(" ", ""),
                    finalStringToCheck));
  }

  public static Optional<String> getToken(Map<String, String> headers) {
    return Utils.getTokenFromCookie(headers).or(() -> Utils.getTokenFromAuthorization(headers));
  }

  private static Optional<String> getTokenFromCookie(Map<String, String> headers) {
    var cookieValue = headers.getOrDefault("cookie", headers.get("Cookie"));

    String[] splitted = Optional.ofNullable(cookieValue).map(s -> s.split(";")).orElse(new String[0]);

    return Arrays.stream(splitted)
        .filter(s -> s.contains(String.format("%s=", Utils.TOKEN_NAME)))
        .findFirst()
        .map(s -> s.split("=")[1]);
  }
}
