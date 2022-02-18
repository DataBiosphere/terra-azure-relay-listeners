package org.broadinstitute.listener.relay.http;

import com.microsoft.azure.relay.HybridConnectionListener;
import org.broadinstitute.listener.relay.transport.TargetResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class ListenerConnectionHandler {

  private final HybridConnectionListener listener;
  private final TargetResolver targetResolver;
  protected final Logger logger = LoggerFactory.getLogger(ListenerConnectionHandler.class);

  public ListenerConnectionHandler(
      @NonNull HybridConnectionListener listener, @NonNull TargetResolver targetResolver) {

    this.listener = listener;
    this.targetResolver = targetResolver;
  }

  public Flux<RelayedHttpRequest> receiveRelayedHttpRequests() {

    return Flux.create(
        sink ->
            listener.setRequestHandler(
                context -> {
                  try {
                    logger.info(
                        "Received HTTP request. URI: {}. Tracking ID:{}",
                        context.getRequest().getUri(),
                        context.getTrackingContext().getTrackingId());
                    sink.next(RelayedHttpRequest.createRelayedHttpRequest(context, targetResolver));
                  } catch (Exception ex) {
                    logger.error(
                        "Error while creating relayed HTTP request. Tracking ID:{}",
                        context.getTrackingContext().getTrackingId(),
                        ex);
                    sink.error(ex);
                  }
                }));
  }

  public Mono<String> openConnection() {
    return Mono.create(
        sink -> {
          logger.info("Opening connection to Azure Relay.");
          listener
              .openAsync()
              .whenComplete(
                  (unused, throwable) -> {
                    if (throwable != null) {
                      logger.error("Failed to open connection to Azure Relay.", throwable);
                      sink.error(throwable);
                      return;
                    }

                    sink.success("Connection successfully established");
                  });
        });
  }

  public Mono<String> closeConnection() {
    return Mono.create(
        sink -> {
          logger.info("Closing connection to Azure Relay.");
          listener
              .closeAsync()
              .whenComplete(
                  (unused, throwable) -> {
                    if (throwable != null) {
                      logger.error("Failed to close connection to Azure Relay.", throwable);
                      sink.error(throwable);
                      return;
                    }

                    sink.success("Connection successfully close");
                  });
        });
  }
}
