package org.broadinstitute.listener.relay.wss;

import com.microsoft.azure.relay.HybridConnectionChannel;
import java.net.http.WebSocket;

public class ConnectionsPair {

  public HybridConnectionChannel getCallerConnection() {
    return callerConnection;
  }

  public WebSocket getLocalWebSocketConnection() {
    return localWebSocketConnection;
  }

  public boolean isConnectionsStateOpen() {
    return !localWebSocketConnection.isInputClosed() && callerConnection.isOpen();
  }

  private final HybridConnectionChannel callerConnection;
  private final WebSocket localWebSocketConnection;

  public ConnectionsPair(
      HybridConnectionChannel callerConnection, WebSocket localWebSocketConnection) {
    this.callerConnection = callerConnection;
    this.localWebSocketConnection = localWebSocketConnection;
  }
}
