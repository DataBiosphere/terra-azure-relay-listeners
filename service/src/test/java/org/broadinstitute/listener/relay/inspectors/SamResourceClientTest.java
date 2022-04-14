package org.broadinstitute.listener.relay.inspectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.broadinstitute.dsde.workbench.client.sam.ApiClient;
import org.broadinstitute.dsde.workbench.client.sam.ApiException;
import org.broadinstitute.dsde.workbench.client.sam.ApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SamResourceClientTest {

  private SamResourceClient samResourceClient;

  @Mock private TokenChecker tokenChecker;
  @Mock private ApiClient apiClient;

  @BeforeEach
  void setUp() throws IOException, InterruptedException, ApiException {
    samResourceClient = new SamResourceClient("resourceId", apiClient, tokenChecker);
  }

  @Test
  void checkWritePermission_sucess() throws IOException, InterruptedException, ApiException {
    var oauthResponse = new OauthInfoResponse();
    oauthResponse.expires_in = 100;
    when(tokenChecker.getOauthInfo(any())).thenReturn(oauthResponse);
    var apiResponse = new ApiResponse(200, Map.of(), true);
    when(apiClient.execute(any(), any())).thenReturn(apiResponse);
    when(apiClient.escapeString(any())).thenReturn("string");

    var firstExpiresAt = samResourceClient.checkWritePermission("accessToken");
    var expectedExpiresAtUpperBound = Instant.now().plusSeconds(oauthResponse.expires_in);

    assertThat(firstExpiresAt.isBefore(expectedExpiresAtUpperBound), equalTo(true));
  }

  @Test
  void checkWritePermission_token_expired() throws IOException, InterruptedException {
    var oauthResponse = new OauthInfoResponse();
    oauthResponse.expires_in = -1;
    when(tokenChecker.getOauthInfo(any())).thenReturn(oauthResponse);

    var res = samResourceClient.checkWritePermission("accessToken");

    assertThat(res, equalTo(Instant.EPOCH));
  }
}
