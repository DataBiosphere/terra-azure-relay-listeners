package org.broadinstitute.listener.config;

public class TargetProperties {
  private boolean removeEntityPathFromWssUri = false;
  private String targetHost;

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
}
