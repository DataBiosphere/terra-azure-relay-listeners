package org.broadinstitute.listener.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "listener")
public class ListenerProperties {

  private String relayConnectionString;
  private String relayConnectionName;
  private String targetHost;

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

  public String getTargetHost() {
    return targetHost;
  }

  public void setTargetHost(String targetHost) {
    this.targetHost = targetHost;
  }
}
