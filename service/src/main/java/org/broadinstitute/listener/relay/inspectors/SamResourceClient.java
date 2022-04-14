package org.broadinstitute.listener.relay.inspectors;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import org.broadinstitute.dsde.workbench.client.sam.ApiClient;
import org.broadinstitute.dsde.workbench.client.sam.ApiException;
import org.broadinstitute.dsde.workbench.client.sam.api.ResourcesApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SamResourceClient {
  private final String samResourceId;
  private final TokenChecker tokenChecker;
  private final ApiClient samClient;

  private final String SAM_RESOURCE_TYPE = "controlled-application-private-workspace-resource";

  private final Logger logger = LoggerFactory.getLogger(SamResourceClient.class);
  // A set of active access token that has passed Sam permission check
  private final HashMap<String, Instant> tokenCache;

  public SamResourceClient(String samResourceId, ApiClient samClient, TokenChecker tokenChecker, HashMap<String, Instant> tokenCache) {
    this.samResourceId = samResourceId;
    this.tokenChecker = tokenChecker;
    this.samClient = samClient;
    this.tokenCache = tokenCache;
  }

  public boolean checkCachedPermission(String accessToken) {
    // If we've seen the token before, we know if it has expired from the info in tokenCache Map;
    // Otherwise, we ask Google and Sam
    if(tokenCache.containsKey(accessToken)) {
      var tokenExpiresAt = tokenCache.get(accessToken);
      var now = Instant.now();
      return tokenExpiresAt.isAfter(now);
    } else {
      tokenCache.remove(accessToken);
      return checkWritePermission(accessToken);
    }
  }

  // Should only be used in checkCachedPermission, but making it public so that we can test it
  public boolean checkWritePermission(String accessToken) {
    samClient.setAccessToken(accessToken);
    var resourceApi = new ResourcesApi(samClient);

    try {
      var oauthInfo = tokenChecker.getOauthInfo(accessToken);
      if(oauthInfo.expires_in > 0) {
        var res = resourceApi.resourcePermissionV2(SAM_RESOURCE_TYPE, samResourceId, "write");
        var expiresAt = Instant.now().plusSeconds(oauthInfo.expires_in);
        if(res) tokenCache.put(accessToken, expiresAt);
        return res;
      } else {
        logger.error("Token expired " + oauthInfo.error);
        return false;
      }
    } catch (IOException e) {
      logger.error("Fail to check token info", e);
      return false;
    } catch (InterruptedException e) {
      logger.error("Fail to check token info", e);
      return false;
    } catch (ApiException e) {
      logger.error("Fail to check Sam permission", e);
      return false;
    }
  }
}


