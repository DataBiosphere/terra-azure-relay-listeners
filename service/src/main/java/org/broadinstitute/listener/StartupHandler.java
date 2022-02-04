package org.broadinstitute.listener;

import org.broadinstitute.listener.relay.transport.RelayedRequestPipeline;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class StartupHandler {

  private final RelayedRequestPipeline relayedRequestPipeline;

  public StartupHandler(RelayedRequestPipeline relayedRequestPipeline) {
    this.relayedRequestPipeline = relayedRequestPipeline;
  }

  @EventListener(ApplicationReadyEvent.class)
  public void startProcessingRequests() {
    this.relayedRequestPipeline.processRelayedRequests();
  }
}
