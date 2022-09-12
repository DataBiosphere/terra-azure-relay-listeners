package org.broadinstitute.listener.config;

import java.util.List;

public class TargetProperties {
  private boolean removeEntityPathFromWssUri = false;
  private boolean removeEntityPathFromHttpUrl = false;
  private String targetHost;
  private List<TargetRoutingRule> targetRoutingRules;

  public boolean isRemoveEntityPathFromWssUri() {
    return removeEntityPathFromWssUri;
  }

  public void setRemoveEntityPathFromWssUri(boolean removeEntityPathFromWssUri) {
    this.removeEntityPathFromWssUri = removeEntityPathFromWssUri;
  }

  public String getTargetHost() {
    return targetHost;
  }

  public void setTargetHost(String targetHost) {
    this.targetHost = targetHost;
  }

  public boolean isRemoveEntityPathFromHttpUrl() {
    return removeEntityPathFromHttpUrl;
  }

  public void setRemoveEntityPathFromHttpUrl(boolean removeEntityPathFromHttpUrl) {
    this.removeEntityPathFromHttpUrl = removeEntityPathFromHttpUrl;
  }

  public List<TargetRoutingRule> getTargetRoutingRules() {
    return targetRoutingRules;
  }

  public void setTargetRoutingRules(List<TargetRoutingRule> targetRoutingRules) {
    this.targetRoutingRules = targetRoutingRules;
  }
}
