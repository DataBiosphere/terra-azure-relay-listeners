package org.broadinstitute.listener.relay.wss;

import com.microsoft.azure.relay.HybridConnectionChannel;
import com.microsoft.azure.relay.WebSocketChannel;
import java.io.IOException;
import java.net.http.WebSocket;
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

  private void relayDataToLocalEndpoint(@NonNull ConnectionsPair connectionsPair) {
    HybridConnectionChannel callerConnection = connectionsPair.getCallerConnection();
    WebSocket localWebSocket = connectionsPair.getLocalWebSocketConnection();

    logger.info("Read and send operation starting");
    while (connectionsPair.isConnectionsStateOpen()) {

      logger.info("Reading from the caller connection.");
      String data = null;
      try {
        data =
            WebSocketTextIOUtils.readTextAsync(
                    (WebSocketChannel) connectionsPair.getCallerConnection())
                .join();
      } catch (Exception e) {
        logger.error("Error while reading data from the caller socket.", e);
      }

      logger.info("Sending data to local connection.");
      localWebSocket.sendText(data, true);
      logger.info("Data sent successfully");
    }

    logger.info("Close remaining connection");
    close(callerConnection, localWebSocket);
  }

  private void close(HybridConnectionChannel callerConnection, WebSocket localWebSocket) {

    if (callerConnection.isOpen()) {
      try {

        logger.info("Attempting to close the caller connection");
        callerConnection.close();
        logger.info("Caller connection is closed");
      } catch (IOException e) {
        logger.error("Failed to close connection", e);
      }
    }

    if (!localWebSocket.isInputClosed()) {
      logger.info("Attempting to close the local connection");
      localWebSocket.sendClose(500, "Remote caller is not available");
      logger.info("Local connection is closed");
    }
  }
}
