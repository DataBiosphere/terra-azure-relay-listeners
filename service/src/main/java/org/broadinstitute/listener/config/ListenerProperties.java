package org.broadinstitute.listener.config;

import java.util.List;
import org.broadinstitute.listener.relay.inspectors.InspectorType;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "listener")
public class ListenerProperties {

  private String relayConnectionString;
  private String relayConnectionName;
  private TargetProperties targetProperties;
  private SamInspectorProperties samInspectorProperties;
  private CorsSupportProperties corsSupportProperties;
  private SetDateAccessedInspectorProperties setDateAccessedInspectorProperties;

  public CorsSupportProperties getCorsSupportProperties() {
    return corsSupportProperties;
  }

  public void setCorsSupportProperties(CorsSupportProperties corsSupportProperties) {
    this.corsSupportProperties = corsSupportProperties;
  }

  public SamInspectorProperties getSamInspectorProperties() {
    return samInspectorProperties;
  }

  public void setSamInspectorProperties(SamInspectorProperties samInspectorProperties) {
    this.samInspectorProperties = samInspectorProperties;
  }

  private List<InspectorType> requestInspectors;

  public String getRelayConnectionString() {
    return relayConnectionString;
  }

  public void setRelayConnectionString(String relayConnectionString) {
    this.relayConnectionString = relayConnectionString;
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

  public SetDateAccessedInspectorProperties getSetDateAccessedInspectorProperties() {
    return setDateAccessedInspectorProperties;
  }

  public void setSetDateAccessedInspectorProperties(
      SetDateAccessedInspectorProperties setDateAccessedInspectorProperties) {
    this.setDateAccessedInspectorProperties = setDateAccessedInspectorProperties;
  }
}
