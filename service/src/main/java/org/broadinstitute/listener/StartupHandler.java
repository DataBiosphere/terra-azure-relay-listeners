package org.broadinstitute.listener;

import org.broadinstitute.listener.relay.ListenerException;
import org.broadinstitute.listener.relay.transport.RelayedRequestPipeline;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.availability.AvailabilityChangeEvent;
import org.springframework.boot.availability.LivenessState;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class StartupHandler {

  private final RelayedRequestPipeline relayedRequestPipeline;
  private final ApplicationContext context;

  private static final Logger logger = LoggerFactory.getLogger(StartupHandler.class);

  public StartupHandler(RelayedRequestPipeline relayedRequestPipeline, ApplicationContext context) {
    this.relayedRequestPipeline = relayedRequestPipeline;
    this.context = context;
  }

  @EventListener(ApplicationReadyEvent.class)
  public void startProcessingRequests() throws ListenerException {
    logger.info("Starting pipeline to process relayed requests ... ");
    try {
      this.relayedRequestPipeline.processRelayedRequests();
      logger.info("Relayed requests pipeline started.");
    } catch (Throwable t) {
      logger.error("Error starting relayed requests pipeline: {}", t.getMessage());
      AvailabilityChangeEvent.publish(context, LivenessState.BROKEN);
      throw new ListenerException("Error during listener startup: " + t.getMessage(), t);
    }
  }
}
