package org.broadinstitute.listener.config;

public class SamInspectorProperties {
  private String samUrl;
  private String samResourceId;
  private String samResourceType = "controlled-application-private-workspace-resource";

  public String getSamUrl() {
    return samUrl;
  }

  public void setSamUrl(String samUrl) {
    this.samUrl = samUrl;
  }

  public String getSamResourceId() {
    return samResourceId;
  }

  public void setSamResourceId(String samResourceId) {
    this.samResourceId = samResourceId;
  }

  public String getSamResourceType() {
    return samResourceType;
  }

  public void setSamResourceType(String samResourceType) {
    this.samResourceType = samResourceType;
  }
}
