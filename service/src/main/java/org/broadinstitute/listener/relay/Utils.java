package org.broadinstitute.listener.relay;

import static com.google.common.net.HttpHeaders.AUTHORIZATION;

import java.util.Map;
import java.util.Optional;

public class Utils {
  public static final String TOKEN_NAME = "LeoToken";
  public static final String SET_COOKIE_API_PATH = "setCookie";

  public static final Optional<String> getTokenFromAuthorization(Map<String, String> headers) {
    var authValue = headers.getOrDefault(AUTHORIZATION, null);

    return Optional.ofNullable(authValue)
        .filter(s -> s.contains("Bearer "))
        .map(s -> s.replaceFirst("Bearer ", "").trim());
  }

  public static boolean isSetCookiePath(java.net.URI uri) {
    var splitted = uri.getPath().split("/");
    if (splitted.length == 3) return splitted[2].equals(SET_COOKIE_API_PATH);
    else return false;
  }
}
