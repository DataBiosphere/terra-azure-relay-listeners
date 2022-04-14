package org.broadinstitute.listener.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

public class SamInspectorProperties {
  private String samUrl;
  private String samResourceId;

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
}
