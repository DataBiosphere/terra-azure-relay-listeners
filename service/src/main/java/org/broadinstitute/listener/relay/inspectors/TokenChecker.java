package org.broadinstitute.listener.relay.inspectors;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.broadinstitute.listener.relay.OauthInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class TokenChecker {
  private final Logger logger = LoggerFactory.getLogger(SamResourceClient.class);
  private final GoogleTokenInfoClient googleTokenInfoClient;

  public TokenChecker(GoogleTokenInfoClient googleTokenInfoClient) {
    this.googleTokenInfoClient = googleTokenInfoClient;
  }

  public OauthInfo getOauthInfo(String token) throws IOException, InterruptedException {
    return getOauthInfoWithAnchorTimestamp(token, Instant.now());
  }

  public OauthInfo getOauthInfoWithAnchorTimestamp(String token, Instant anchor)
      throws IOException, InterruptedException {
    var jwt = tryDecodeAsB2CToken(token);
    if (jwt.isPresent()) {
      var jwtExpiration = jwt.get().getExpiresAt().toInstant();
      if (jwtExpiration.isAfter(anchor)) {
        var stringClaims = new HashMap<String, String>();
        jwt.get().getClaims().forEach((k, v) -> stringClaims.put(k, v.asString()));
        return new OauthInfo(Optional.of(jwtExpiration), "", stringClaims);
      } else {
        return new OauthInfo(Optional.empty(), "JWT expired", Map.of());
      }
    } else {
      var googleTokenInfo = googleTokenInfoClient.getTokenInfo(token);
      if (googleTokenInfo.expires_in > 0) {
        return new OauthInfo(
            Optional.of(anchor.plusSeconds(googleTokenInfo.expires_in)),
            googleTokenInfo.error,
            claimsFromGoogleInfo(googleTokenInfo));
      } else {
        return new OauthInfo(Optional.empty(), googleTokenInfo.error, Map.of());
      }
    }
  }

  private Map<String, String> claimsFromGoogleInfo(GoogleOauthInfoResponse response) {
    var claims = new HashMap<>(Map.of("sub", response.user_id));
    if (response.email != null) {
      claims.put("email", response.email);
    }

    return claims;
  }

  private Optional<DecodedJWT> tryDecodeAsB2CToken(String token) {
    try {
      var decoded = JWT.decode(token);
      return Optional.of(decoded); // .getExpiresAt().toInstant());
    } catch (com.auth0.jwt.exceptions.JWTDecodeException e) {
      logger.debug("Fail to decode JWT", e);
      return Optional.empty();
    }
  }
}
