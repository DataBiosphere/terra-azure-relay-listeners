package org.broadinstitute.listener.relay.wss;

import com.microsoft.azure.relay.HybridConnectionChannel;
import com.microsoft.azure.relay.WebSocketChannel;
import java.io.IOException;
import java.net.http.WebSocket;
import java.net.http.WebSocket.Listener;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletionStage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;

public class TargetWebSocketListener implements Listener {

  private final Logger logger = LoggerFactory.getLogger(TargetWebSocketListener.class);
  private final HybridConnectionChannel connectionChannel;
  private final String trackingId;

  public TargetWebSocketListener(@NonNull HybridConnectionChannel connectionChannel) {
    this.connectionChannel = connectionChannel;
    trackingId = connectionChannel.getTrackingContext().getTrackingId();
  }

  @Override
  public CompletionStage<?> onPong(WebSocket webSocket, ByteBuffer message) {
    logger.info("OnPong: {}", message);
    return Listener.super.onPong(webSocket, message);
  }

  @Override
  public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
    logger.info(
        "WebSocket closed. Status code:{}, Reason:{}. Tracking ID:{}",
        statusCode,
        reason,
        trackingId);
    try {
      connectionChannel.close();
    } catch (IOException e) {
      logger.error("Failed to close caller connection in the listener", e);
    }

    return Listener.super.onClose(webSocket, statusCode, reason);
  }

  @Override
  public void onError(WebSocket webSocket, Throwable error) {
    logger.error("Error WebSocket Target Listener", error);
  }

  @Override
  public CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer data, boolean last) {
    // the buffer can't live beyond this method, hence cloning the data.
    if (connectionChannel.isOpen()) {
      logger.info("Relayed connection is open. Writing binary data. Tracking ID:{}", trackingId);
      connectionChannel.writeAsync(data).join();
    }
    return Listener.super.onBinary(webSocket, data, last);
  }

  @Override
  public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {

    if (connectionChannel.isOpen()) {
      logger.info("Relayed connection is open. Writing text data. Tracking ID:{}", trackingId);

      try {
        WebSocketTextIOUtils.writeTextAsync(
                (WebSocketChannel) connectionChannel, data.toString(), last)
            .join();
        logger.info(
            "Successfully wrote {} bytes to caller from the target. Tracking ID:{}",
            data.length(),
            trackingId);
      } catch (Exception e) {
        logger.error(
            "Error while attempting to write data to the caller. Tracking ID:{}", trackingId, e);
      }
    }

    return Listener.super.onText(webSocket, data, last);
  }
}
