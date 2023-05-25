package org.broadinstitute.listener.relay.inspectors;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import java.time.Instant;
import java.util.Date;

public class TokenUtils {

  public static String buildJWT(Instant anchor) {
    var jwt = JWT.create();
    Algorithm algorithm = Algorithm.HMAC256("testing");

    return jwt.withExpiresAt(Date.from(anchor))
        .withClaim("email", "example@example.com")
        .withClaim("idtyp", "app")
        .withSubject("123ABC")
        .withIssuer("fake")
        .sign(algorithm);
  }
}
