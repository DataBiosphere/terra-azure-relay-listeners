package org.broadinstitute.listener.relay.transport;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.listener.config.ListenerProperties;
import org.springframework.stereotype.Component;

@Component
public class TargetHostResolverFromConfiguration implements TargetHostResolver {

  private final ListenerProperties properties;

  public TargetHostResolverFromConfiguration(ListenerProperties properties) {
    this.properties = properties;
  }

  @Override
  public String resolveTargetHost() {
    if (StringUtils.isBlank(properties.getTargetHost())) {
      throw new IllegalStateException("The target host configuration is missing.");
    }

    return properties.getTargetHost();
  }
}
