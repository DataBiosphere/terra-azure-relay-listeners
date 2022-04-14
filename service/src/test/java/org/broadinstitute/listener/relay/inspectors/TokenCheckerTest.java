package org.broadinstitute.listener.relay.inspectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.io.IOException;
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
    assertThat(res.error, equalTo("invalid_token"));
  }
}
