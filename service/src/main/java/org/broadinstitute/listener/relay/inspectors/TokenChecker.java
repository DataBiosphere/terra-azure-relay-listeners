package org.broadinstitute.listener.relay.inspectors;

import com.auth0.jwt.JWT;
import com.google.gson.Gson;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TokenChecker {
  private final HttpClient httpClient = HttpClient.newHttpClient();
  private final String GOOGLE_OAUTH_SERVER = "https://www.googleapis.com/oauth2/v1/tokeninfo?access_token=";
  private final Logger logger = LoggerFactory.getLogger(SamResourceClient.class);

  public OauthInfo getOauthInfo(String token) throws IOException, InterruptedException {
    var jwtExpiration = tryDecodeAsB2cToken(token);

    var now = Instant.now();

    if (jwtExpiration.isPresent()) {
      if(jwtExpiration.get().isAfter(now))
        return new OauthInfo(Optional.of(jwtExpiration.get()), "");
      else
        return new OauthInfo(Optional.empty(), "JWT expired");
    } else {
      var request = HttpRequest.newBuilder()
          .uri(URI.create(GOOGLE_OAUTH_SERVER + token))
          .build();

      var oauthInfoResponse = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      var decoded = new Gson().fromJson(oauthInfoResponse.body(), GoogleOauthInfoResponse.class);

      if(decoded.expires_in > 0)
          return new OauthInfo(Optional.of(now.plusSeconds(decoded.expires_in)), decoded.error);
      else
        return new OauthInfo(Optional.empty(), decoded.error);
    }
  }

  private Optional<Instant> tryDecodeAsB2cToken(String token) {
    try {
      var decoded = JWT.decode(token);
      return Optional.of(decoded.getExpiresAt().toInstant());
    } catch (com.auth0.jwt.exceptions.JWTDecodeException e) {
      logger.info("Fail to check decode JWT", e);
      return Optional.empty();
    }
  }
}

class GoogleOauthInfoResponse {
  int expires_in;
  String error;
}

record OauthInfo(Optional<Instant> expiresAt, String error) {}
