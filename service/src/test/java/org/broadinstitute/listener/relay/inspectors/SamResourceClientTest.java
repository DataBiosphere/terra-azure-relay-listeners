package org.broadinstitute.listener.relay.inspectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.broadinstitute.dsde.workbench.client.sam.ApiClient;
import org.broadinstitute.dsde.workbench.client.sam.ApiException;
import org.broadinstitute.dsde.workbench.client.sam.ApiResponse;
import org.broadinstitute.dsde.workbench.client.sam.model.UserStatusInfo;
import org.broadinstitute.listener.relay.OauthInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SamResourceClientTest {

  private TokenChecker tokenChecker = mock(TokenChecker.class);
  @Mock private ApiClient apiClient;

  @Spy
  private SamResourceClient samResourceClient =
      new SamResourceClient(
          UUID.randomUUID(), "samUrl", "resourceId", "resourceType", tokenChecker, "myaction");

  @BeforeEach
  void setUp() throws IOException, InterruptedException, ApiException {
    doReturn(apiClient).when(samResourceClient).getApiClient(anyString());
  }

  @Test
  void checkPermission_success() throws IOException, InterruptedException, ApiException {
    var expiresAt = Instant.now().plusSeconds(100);
    var oauthResponse =
        new OauthInfo(Optional.of(expiresAt), "", Map.of("email", "example@example.com"));
    when(tokenChecker.getOauthInfo(any())).thenReturn(oauthResponse);
    var apiResponse = new ApiResponse(200, Map.of(), true);
    when(apiClient.execute(any(), any())).thenReturn(apiResponse);
    when(apiClient.escapeString(any())).thenReturn("string");

    var expiresAtAfterPermissionCheck = samResourceClient.checkPermission("accessToken");

    assertThat(expiresAtAfterPermissionCheck, equalTo(expiresAt));
  }

  @Test
  void checkPermission_token_expired() throws IOException, InterruptedException {
    var oauthResponse =
        new OauthInfo(
            Optional.of(Instant.now().minusSeconds(100)),
            "",
            Map.of("email", "example@example.com"));
    when(tokenChecker.getOauthInfo(any())).thenReturn(oauthResponse);

    var res = samResourceClient.checkPermission("accessToken");

    assertThat(res, equalTo(Instant.EPOCH));
  }

  @Test
  void checkPermission_no_workspace_access()
      throws IOException, InterruptedException, ApiException {
    var expiresAt = Instant.now().plusSeconds(100);
    var oauthResponse =
        new OauthInfo(Optional.of(expiresAt), "", Map.of("email", "example@example.com"));
    // check for workspace read access returns false
    var apiResponse = new ApiResponse(200, Map.of(), false);

    when(tokenChecker.getOauthInfo(any())).thenReturn(oauthResponse);
    when(apiClient.execute(any(), any())).thenReturn(apiResponse);
    when(apiClient.escapeString(any())).thenReturn("string");

    var res = samResourceClient.checkPermission("accessToken");
    assertThat(res, equalTo(Instant.EPOCH));
  }

  @Test
  void isUserEnabled_enabled() throws ApiException {
    var apiResponse = new ApiResponse(200, Map.of(), new UserStatusInfo().enabled(true));
    when(apiClient.execute(any(), any())).thenReturn(apiResponse);
    var res = samResourceClient.isUserEnabled("token");
    assertTrue(res);
  }

  @Test
  void isUserEnabled_disabled() throws ApiException {
    var apiResponse = new ApiResponse(200, Map.of(), new UserStatusInfo().enabled(false));
    when(apiClient.execute(any(), any())).thenReturn(apiResponse);
    var res = samResourceClient.isUserEnabled("token");
    assertFalse(res);
  }

  @Test
  void isUserEnabled_error() throws ApiException {
    doThrow(new ApiException()).when(apiClient).execute(any(), any());
    var res = samResourceClient.isUserEnabled("token");
    assertFalse(res);
  }
}
