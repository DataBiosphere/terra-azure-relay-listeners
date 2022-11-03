package org.broadinstitute.listener.config;

public final class SamInspectorProperties {

  private static final String DEFAULT_SAM_ACTION = "write";
  private String samUrl;
  private String samResourceId;
  private String samResourceType;
  private String samAction = DEFAULT_SAM_ACTION;

  public String getSamUrl() {
    return samUrl;
  }

  public String getSamResourceId() {
    return samResourceId;
  }

  public String getSamResourceType() {
    return samResourceType;
  }

  public String getSamAction() {
    return samAction;
  }

  public void setSamAction(String samAction) {
    this.samAction = samAction;
  }

  public void setSamUrl(String samUrl) {
    this.samUrl = samUrl;
  }

  public void setSamResourceId(String samResourceId) {
    this.samResourceId = samResourceId;
  }

  public void setSamResourceType(String samResourceType) {
    this.samResourceType = samResourceType;
  }
}
