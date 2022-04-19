package org.broadinstitute.listener.relay.inspectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.io.IOException;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TokenCheckerTest {

  private TokenChecker tokenChecker;

  @BeforeEach
  void setUp() {
    tokenChecker = new TokenChecker();
  }

  @Test
  void checkWritePermission() throws IOException, InterruptedException {
    var res = tokenChecker.getOauthInfo("");
    assertThat(res.error(), equalTo("invalid_token"));
    assertThat(res.expiresAt(), equalTo(Optional.empty()));
  }

//  @Test
//  void checkWritePermission_b2c() throws IOException, InterruptedException {
//    var res = tokenChecker.getOauthInfo("<token>");
//    assertThat(res.error(), equalTo(""));
//    assertThat(res.expiresAt(), equalTo(Optional.of(Instant.ofEpochMilli(1650461564))));
//  }
}
