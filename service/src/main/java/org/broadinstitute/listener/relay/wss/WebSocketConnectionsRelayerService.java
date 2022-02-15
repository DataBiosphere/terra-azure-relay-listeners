package org.broadinstitute.listener.relay.wss;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
public class WebSocketConnectionsRelayerService {

  private final Logger logger = LoggerFactory.getLogger(WebSocketConnectionsRelayerService.class);
  private final ExecutorService executorService = Executors.newCachedThreadPool();

  public void startDataRelay(@NonNull ConnectionsPair connectionsPair) {
    logger.info("Submitting read operation");
    executorService.execute(() -> relayDataToLocalEndpoint(connectionsPair));
  }

  public void stopService() {
    executorService.shutdown();
  }

  public void readAndSendText(@NonNull ConnectionsPair connectionsPair) {

    logger.info("Reading from the caller connection.");
    String data = connectionsPair.readTextFromCaller();

    logger.info("Sending data to local connection.");
    connectionsPair.sendTextToLocalWebSocket(data);
    logger.info("Data sent successfully");
  }

  public void relayDataToLocalEndpoint(@NonNull ConnectionsPair connectionsPair) {

    logger.info("Read and send operation starting");
    while (connectionsPair.isConnectionsStateOpen()) {

      try {
        readAndSendText(connectionsPair);
      } catch (Exception e) {
        logger.error("Error while reading data from the caller socket.", e);
        connectionsPair.close();
      }
    }

    logger.info("Close remaining connection");
    // calling the method again if one of the connections in the
    // pair is still open.
    // as it checks if the connection is open before closing it.
    connectionsPair.close();
  }
}
