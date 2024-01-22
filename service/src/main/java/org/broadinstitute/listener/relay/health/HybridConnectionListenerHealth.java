package org.broadinstitute.listener.relay.health;

import com.microsoft.azure.relay.HybridConnectionListener;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health.Builder;
import org.springframework.stereotype.Component;

/**
 * Contributes to Actuator health reporting. This health check signals if the
 * HybridConnectionListener used by this listener is currently online.
 */
@Component
public class HybridConnectionListenerHealth extends AbstractHealthIndicator {

  private final HybridConnectionListener listener;

  public HybridConnectionListenerHealth(HybridConnectionListener listener) {
    this.listener = listener;
  }

  @Override
  protected void doHealthCheck(Builder builder) throws Exception {
    try {
      if (listener.isOnline()) {
        builder.up();
      } else {
        builder.down().withDetail("reason", "HybridConnectionListener is offline");
      }
    } catch (Exception ex) {
      builder.down(ex);
    }
  }
}
