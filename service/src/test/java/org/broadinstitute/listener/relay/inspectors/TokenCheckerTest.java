package org.broadinstitute.listener.relay.inspectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TokenCheckerTest {

  private TokenChecker tokenChecker;
  @Mock GoogleTokenInfoClient googleTokenInfoClient;

  @BeforeEach
  void setUp() {
    tokenChecker = new TokenChecker(googleTokenInfoClient);
  }

  @Test
  void checkWritePermission_invalidToken() throws IOException, InterruptedException {
    var googleResponse = new GoogleOauthInfoResponse();
    googleResponse.error = "invalid_token";
    when(googleTokenInfoClient.getTokenInfo(anyString())).thenReturn(googleResponse);

    var res = tokenChecker.getOauthInfo("");

    assertThat(res.error(), equalTo("invalid_token"));
    assertThat(res.expiresAt(), equalTo(Optional.empty()));
  }

  @Test
  void checkWritePermission_validToken() throws IOException, InterruptedException {
    var googleResponse = new GoogleOauthInfoResponse();
    googleResponse.expires_in = 300;
    googleResponse.user_id = "1234";
    var anchor = LocalDateTime.parse("2023-05-23T11:50:55").toInstant(ZoneOffset.UTC);
    when(googleTokenInfoClient.getTokenInfo(anyString())).thenReturn(googleResponse);

    var res = tokenChecker.getOauthInfoWithAnchorTimestamp("", anchor);

    assertThat(res.error(), is(nullValue()));
    assertThat(res.expiresAt(), equalTo(Optional.of(anchor.plusSeconds(300))));
  }

  @Test
  void checkWritePermission_b2c() throws IOException, InterruptedException {
    var anchor = LocalDateTime.parse("2023-05-23T11:50:55").toInstant(ZoneOffset.UTC);
    var token = TokenTestUtils.buildJWT(anchor.plusSeconds(120));
    var res = tokenChecker.getOauthInfoWithAnchorTimestamp(token, anchor);

    assertThat(res.error(), equalTo(""));
    assertThat(res.expiresAt(), equalTo(Optional.of(anchor.plusSeconds(120))));
    assertThat(res.claims().get("email"), equalTo("example@example.com"));
  }

  @Test
  void checkWritePermission_b2c_expired() throws IOException, InterruptedException {
    var anchor = LocalDateTime.parse("2023-05-23T11:50:55").toInstant(ZoneOffset.UTC);
    var token = TokenTestUtils.buildJWT(anchor);
    var res = tokenChecker.getOauthInfoWithAnchorTimestamp(token, anchor);

    assertThat(res.error(), equalTo("JWT expired"));
    assertThat(res.expiresAt(), equalTo(Optional.empty()));
  }
}
