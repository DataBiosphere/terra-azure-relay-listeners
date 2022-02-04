package org.broadinstitute.listener.relay.wss;

import com.microsoft.azure.relay.HybridConnectionChannel;
import com.microsoft.azure.relay.WebSocketChannel;
import java.net.http.WebSocket;
import java.net.http.WebSocket.Listener;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletionStage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LocalWebSocketListener implements Listener {

  private final Logger logger = LoggerFactory.getLogger(LocalWebSocketListener.class);
  private final HybridConnectionChannel connectionChannel;

  public LocalWebSocketListener(HybridConnectionChannel connectionChannel) {
    this.connectionChannel = connectionChannel;
  }

  @Override
  public CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer data, boolean last) {
    // the buffer can't live beyond this method, hence cloning the data.
    if (connectionChannel.isOpen()) {
      logger.info("Relayed connection is open. Writing binary data.");
      connectionChannel.writeAsync(data).join();
    }
    return Listener.super.onBinary(webSocket, data, last);
  }

  @Override
  public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {

    if (connectionChannel.isOpen()) {
      logger.info("Relayed connection is open. Writing text data.");

      try {
        WebSocketTextIOUtils.writeTextAsync((WebSocketChannel) connectionChannel, data.toString())
            .join();
      } catch (Exception e) {
        logger.error("Error while attempting to write data to the caller", e);
      }
    }

    return Listener.super.onText(webSocket, data, last);
  }
}
