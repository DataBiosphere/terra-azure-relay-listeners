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
    logger.info("Submitting read operation. Tracking ID:{}", connectionsPair.getTrackingId());
    executorService.execute(() -> relayDataToLocalEndpoint(connectionsPair));
  }

  public void stopService() {
    executorService.shutdown();
  }

  public void readAndSendText(@NonNull ConnectionsPair connectionsPair) {

    String trackingId = connectionsPair.getTrackingId();

    logger.info("Reading from the caller connection. Tracking ID:{}", trackingId);
    String data = connectionsPair.readTextFromCaller();

    if (data == null) {
      logger.info("Received null data from the caller. Tracking ID:{}", trackingId);
      return;
    }

    if (data != null) {

      logger.info("Read {} bytes from the caller. Tracking ID:{}", data.length(), trackingId);

      connectionsPair.sendTextToLocalWebSocket(data);

      logger.info(
          "Sent {} bytes to the target connection. Tracking ID:{} ", data.length(), trackingId);
    }
  }

  public void relayDataToLocalEndpoint(@NonNull ConnectionsPair connectionsPair) {

    logger.info(
        "Read and send operation starting. Tracking ID:{}", connectionsPair.getTrackingId());
    while (connectionsPair.isConnectionsStateOpen()) {

      try {
        readAndSendText(connectionsPair);
      } catch (Exception e) {
        logger.error(
            "Error while reading data from the caller socket. Tracking ID:{}",
            connectionsPair.getTrackingId(),
            e);
        connectionsPair.close();
      }
    }

    logger.info(
        "Stopped WebSocket relay processing. Connections are not open. Tracking ID:{}",
        connectionsPair.getTrackingId());

    // calling the method again if one of the connections in the
    // pair is still open.
    // It is okay to make the call again as it checks if the connection is open before closing it.
    connectionsPair.close();
    logger.info("Closed connection pair. Tracking ID:{}", connectionsPair.getTrackingId());
  }
}
