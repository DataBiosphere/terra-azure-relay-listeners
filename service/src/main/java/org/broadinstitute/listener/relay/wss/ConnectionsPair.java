package org.broadinstitute.listener.relay.wss;

import com.microsoft.azure.relay.HybridConnectionChannel;
import com.microsoft.azure.relay.WebSocketChannel;
import java.io.IOException;
import java.net.http.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;

public class ConnectionsPair {

  private final Logger logger = LoggerFactory.getLogger(WebSocketConnectionsRelayerService.class);

  public HybridConnectionChannel getCallerConnection() {
    return callerConnection;
  }

  public WebSocket getLocalWebSocketConnection() {
    return localWebSocketConnection;
  }

  public String getTrackingId() {
    if (callerConnection == null || callerConnection.getTrackingContext() == null) {
      return "";
    }

    return callerConnection.getTrackingContext().getTrackingId();
  }

  private final HybridConnectionChannel callerConnection;
  private final WebSocket localWebSocketConnection;

  public boolean isConnectionsStateOpen() {
    return !localWebSocketConnection.isInputClosed() && callerConnection.isOpen();
  }

  public ConnectionsPair(
      @NonNull HybridConnectionChannel callerConnection,
      @NonNull WebSocket localWebSocketConnection) {
    this.callerConnection = callerConnection;
    this.localWebSocketConnection = localWebSocketConnection;
  }

  public String readTextFromCaller() {
    try {
      return WebSocketTextIOUtils.readTextAsync((WebSocketChannel) callerConnection).join();
    } catch (Exception e) {
      throw new RuntimeException("Failed to read data from the caller connection", e);
    }
  }

  public void sendTextToLocalWebSocket(String data) {
    localWebSocketConnection.sendText(data, true);
  }

  public void close() {

    if (callerConnection.isOpen()) {
      try {
        logger.debug("Caller connection is open. Attempting to close the caller connection");
        callerConnection.close();
        logger.debug("Caller connection is closed");
      } catch (IOException e) {
        logger.error("Failed to close connection", e);
      }
    }

    if (!localWebSocketConnection.isInputClosed()) {
      logger.debug("WebSocket connection is open. Attempting to close the local connection");
      localWebSocketConnection.sendClose(500, "Remote caller is not available");
      logger.debug("Local connection is closed");
    }
  }
}
