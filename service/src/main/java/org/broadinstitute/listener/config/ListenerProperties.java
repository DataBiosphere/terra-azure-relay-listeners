package org.broadinstitute.listener.config;

import java.util.List;
import org.broadinstitute.listener.relay.inspectors.InspectorType;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "listener")
public class ListenerProperties {

  private String relayConnectionString;
  private String relayConnectionName;
  private String samUrl;
  private String samResourceId;
  private TargetProperties targetProperties;
  private List<InspectorType> requestInspectors;

  public String getRelayConnectionString() {
    return relayConnectionString;
  }

  public void setRelayConnectionString(String relayConnectionString) {
    this.relayConnectionString = relayConnectionString;
  }

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

  public String getRelayConnectionName() {
    return relayConnectionName;
  }

  public void setRelayConnectionName(String relayConnectionName) {
    this.relayConnectionName = relayConnectionName;
  }

  public TargetProperties getTargetProperties() {
    return targetProperties;
  }

  public void setTargetProperties(TargetProperties targetProperties) {
    this.targetProperties = targetProperties;
  }

  public List<InspectorType> getRequestInspectors() {
    return requestInspectors;
  }

  public void setRequestInspectors(List<InspectorType> requestInspectors) {
    this.requestInspectors = requestInspectors;
  }
}
