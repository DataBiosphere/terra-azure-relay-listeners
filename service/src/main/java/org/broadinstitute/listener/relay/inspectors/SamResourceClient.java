package org.broadinstitute.listener.relay.inspectors;

import java.io.IOException;
import java.time.Instant;
import org.broadinstitute.dsde.workbench.client.sam.ApiClient;
import org.broadinstitute.dsde.workbench.client.sam.ApiException;
import org.broadinstitute.dsde.workbench.client.sam.api.ResourcesApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;

public class SamResourceClient {
  private final String samResourceId;
  private final String samResourceType;
  private final TokenChecker tokenChecker;
  private final ApiClient samClient;
  private final String samAction;

  private final Logger logger = LoggerFactory.getLogger(SamResourceClient.class);

  public SamResourceClient(
      String samResourceId,
      String samResourceType,
      ApiClient samClient,
      TokenChecker tokenChecker,
      String samAction) {
    this.samResourceId = samResourceId;
    this.samResourceType = samResourceType;
    this.tokenChecker = tokenChecker;
    this.samClient = samClient;
    this.samAction = samAction;
  }

  // Should only be used in checkCachedPermission, but making it public so that we can test it
  @Cacheable("expiresAt")
  public Instant checkPermission(String accessToken) {
    try {
      var oauthInfo = tokenChecker.getOauthInfo(accessToken);
      if (oauthInfo.expiresAt().isPresent()) {

        samClient.setAccessToken(accessToken);
        var resourceApi = new ResourcesApi(samClient);

        // check that user has access to workspace
        boolean workspaceAccess = resourceApi.resourcePermissionV2("workspace", samResourceId, "read");
        if (!workspaceAccess) {
          logger.error("Unauthorized request. User doesn't have access to workspace");
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
}
