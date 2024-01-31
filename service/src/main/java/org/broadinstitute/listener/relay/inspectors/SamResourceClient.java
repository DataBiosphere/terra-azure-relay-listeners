package org.broadinstitute.listener.relay.inspectors;

import com.google.common.annotations.VisibleForTesting;
import java.io.IOException;
import java.time.Instant;
import java.util.UUID;
import okhttp3.OkHttpClient;
import org.broadinstitute.dsde.workbench.client.sam.ApiClient;
import org.broadinstitute.dsde.workbench.client.sam.ApiException;
import org.broadinstitute.dsde.workbench.client.sam.api.ResourcesApi;
import org.broadinstitute.dsde.workbench.client.sam.api.UsersApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;

public class SamResourceClient {
  private final UUID workspaceId;
  private final String samUrl;
  private final String samResourceId;
  private final String samResourceType;
  private final TokenChecker tokenChecker;
  private final String samAction;
  private final OkHttpClient commonHttpClient;

  private final Logger logger = LoggerFactory.getLogger(SamResourceClient.class);

  public SamResourceClient(
      UUID workspaceId,
      String samUrl,
      String samResourceId,
      String samResourceType,
      TokenChecker tokenChecker,
      String samAction) {
    this.workspaceId = workspaceId;
    this.samUrl = samUrl;
    this.samResourceId = samResourceId;
    this.samResourceType = samResourceType;
    this.tokenChecker = tokenChecker;
    this.samAction = samAction;
    this.commonHttpClient = new ApiClient().getHttpClient().newBuilder().build();
  }

  // Should only be used in checkCachedPermission, but making it public so that we can test it
  @Cacheable("expiresAt")
  public Instant checkPermission(String accessToken) {
    try {
      var oauthInfo = tokenChecker.getOauthInfo(accessToken);
      // TODO REMOVEME
      logger.info("JWT = {}", accessToken);
      if (oauthInfo.expiresAt().isPresent()) {

        var apiClient = getApiClient(accessToken);
        var resourceApi = new ResourcesApi(apiClient);

        // check that user has access to workspace
        boolean workspaceAccess =
            resourceApi.resourcePermissionV2("workspace", workspaceId.toString(), "read");
        if (!workspaceAccess) {
          logger.error("Unauthorized request. User doesn't have access to workspace. Info = {}");
          return Instant.EPOCH;
        }

        var res = resourceApi.resourcePermissionV2(samResourceType, samResourceId, samAction);
        if (res) return oauthInfo.expiresAt().get();
        else {
          logger.error("unauthorized request");
          return Instant.EPOCH;
        }
      } else {
        logger.error("Token expired " + oauthInfo.error());
        return Instant.EPOCH;
      }
    } catch (IOException | InterruptedException e) {
      logger.error("Fail to check token info", e);
      return Instant.EPOCH;
    } catch (ApiException e) {
      logger.error("Fail to check Sam permission", e);
      return Instant.EPOCH;
    } catch (Exception e) {
      logger.error("Fail for unknown reasons", e);
      return Instant.EPOCH;
    }
  }

  /**
   * Checks if the given user is enabled in Sam and has accepted the Terms of Service.
   *
   * @param accessToken user token
   * @return true if the user is enabled; false otherwise.
   */
  public boolean isUserEnabled(String accessToken) {
    var apiClient = getApiClient(accessToken);
    var usersApi = new UsersApi(apiClient);
    try {
      var userInfo = usersApi.getUserStatusInfo();
      // Note getEnabled() also includes whether the user has accepted the Terms of Service
      return userInfo.getEnabled();
    } catch (ApiException e) {
      logger.error("Fail to check Sam permission", e);
      return false;
    } catch (Exception e) {
      logger.error("Fail for unknown reasons", e);
      return false;
    }
  }

  @VisibleForTesting
  ApiClient getApiClient(String accessToken) {
    // OkHttpClient objects manage their own thread pools, so it's much more performant to share one
    // across requests.
    ApiClient apiClient = new ApiClient().setHttpClient(commonHttpClient).setBasePath(samUrl);
    apiClient.setAccessToken(accessToken);
    return apiClient;
  }
}
