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

  private final Logger logger = LoggerFactory.getLogger(SamResourceClient.class);

  public SamResourceClient(
      String samResourceId,
      String samResourceType,
      ApiClient samClient,
      TokenChecker tokenChecker) {
    this.samResourceId = samResourceId;
    this.samResourceType = samResourceType;
    this.tokenChecker = tokenChecker;
    this.samClient = samClient;
  }

  // Should only be used in checkCachedPermission, but making it public so that we can test it
  @Cacheable("expiresAt")
  public Instant checkWritePermission(String accessToken) {
    try {
      var oauthInfo = tokenChecker.getOauthInfo(accessToken);
      if (oauthInfo.expiresAt().isPresent()) {

        samClient.setAccessToken(accessToken);
        var resourceApi = new ResourcesApi(samClient);

        var res = resourceApi.resourcePermissionV2(samResourceType, samResourceId, "write");
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
