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
  private HashMap<String, Instant> tokenCache = new HashMap<String, Instant>();

  @Mock private TokenChecker tokenChecker;
  @Mock private ApiClient apiClient;

  @BeforeEach
  void setUp() throws IOException, InterruptedException, ApiException {
    samResourceClient = new SamResourceClient("resourceId", apiClient, tokenChecker, tokenCache);
  }

  @Test
  void checkWritePermission_sucess() throws IOException, InterruptedException, ApiException {
    var oauthResponse = new OauthInfoResponse();
    oauthResponse.expires_in = 100;
    when(tokenChecker.getOauthInfo(any())).thenReturn(oauthResponse);
    var apiResponse = new ApiResponse(200, Map.of(), true);
    when(apiClient.execute(any(), any())).thenReturn(apiResponse);
    when(apiClient.escapeString(any())).thenReturn("string");

    var res = samResourceClient.checkWritePermission("accessToken");

    assertThat(res, equalTo(true));
    var expectedExpiresAt = Instant.now().plusSeconds(100);
    assertThat(tokenCache.get("accessToken").isBefore(expectedExpiresAt), equalTo(true));
  }

  @Test
  void checkWritePermission_token_expired() throws IOException, InterruptedException, ApiException {
    var oauthResponse = new OauthInfoResponse();
    oauthResponse.expires_in = -1;
    when(tokenChecker.getOauthInfo(any())).thenReturn(oauthResponse);

    var res = samResourceClient.checkWritePermission("accessToken");

    assertThat(res, equalTo(false));
    assertThat(tokenCache.containsKey("accessToken"), equalTo(false));
  }

  @Test
  void checkCachedPermission_success() {
    var tokenCache = new HashMap<String, Instant>();
    tokenCache.put("accessToken", Instant.now().plusSeconds(100));
    var samResourceClient = new SamResourceClient("resourceId", apiClient, tokenChecker, tokenCache);

    var res = samResourceClient.checkCachedPermission("accessToken");

    assertThat(res, equalTo(true));
  }

  @Test
  void checkCachedPermission_token_expired() {
    var tokenCache = new HashMap<String, Instant>();
    tokenCache.put("accessToken", Instant.now().minusSeconds(100));
    var samResourceClient = new SamResourceClient("resourceId", apiClient, tokenChecker, tokenCache);

    var res = samResourceClient.checkCachedPermission("accessToken");

    assertThat(res, equalTo(false));
  }

  @Test
  void checkCachedPermission_token_not_in_cache() throws IOException, InterruptedException {
    var oauthResponse = new OauthInfoResponse();
    oauthResponse.expires_in = -1;
    when(tokenChecker.getOauthInfo(any())).thenReturn(oauthResponse);
    var res = samResourceClient.checkCachedPermission("accessToken1");

    assertThat(res, equalTo(false));
  }
}
