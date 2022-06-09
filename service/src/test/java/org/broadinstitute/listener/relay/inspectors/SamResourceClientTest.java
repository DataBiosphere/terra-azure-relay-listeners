package org.broadinstitute.listener.relay.inspectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import org.broadinstitute.dsde.workbench.client.sam.ApiClient;
import org.broadinstitute.dsde.workbench.client.sam.ApiException;
import org.broadinstitute.dsde.workbench.client.sam.ApiResponse;
import org.broadinstitute.listener.relay.OauthInfo;
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
    samResourceClient =
        new SamResourceClient("resourceId", "resourceType", apiClient, tokenChecker);
  }

  @Test
  void checkWritePermission_sucess() throws IOException, InterruptedException, ApiException {
    var expiresAt = Instant.now().plusSeconds(100);
    var oauthResponse = new OauthInfo(Optional.of(expiresAt), "");
    when(tokenChecker.getOauthInfo(any())).thenReturn(oauthResponse);
    var apiResponse = new ApiResponse(200, Map.of(), true);
    when(apiClient.execute(any(), any())).thenReturn(apiResponse);
    when(apiClient.escapeString(any())).thenReturn("string");

    var expiresAtAfterPermissionCheck = samResourceClient.checkWritePermission("accessToken");

    assertThat(expiresAtAfterPermissionCheck, equalTo(expiresAt));
  }

  @Test
  void checkWritePermission_token_expired() throws IOException, InterruptedException {
    var oauthResponse = new OauthInfo(Optional.of(Instant.now().minusSeconds(100)), "");
    when(tokenChecker.getOauthInfo(any())).thenReturn(oauthResponse);

    var res = samResourceClient.checkWritePermission("accessToken");

    assertThat(res, equalTo(Instant.EPOCH));
  }
}
