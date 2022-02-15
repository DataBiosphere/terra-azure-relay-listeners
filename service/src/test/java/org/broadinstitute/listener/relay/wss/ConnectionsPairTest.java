package org.broadinstitute.listener.relay.wss;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.microsoft.azure.relay.HybridConnectionChannel;
import java.io.IOException;
import java.net.http.WebSocket;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConnectionsPairTest {

  @Mock private HybridConnectionChannel callerConnection;
  @Mock private WebSocket targetWebSocket;
  private ConnectionsPair connectionsPair;

  @BeforeEach
  void setUp() {
    connectionsPair = new ConnectionsPair(callerConnection, targetWebSocket);
  }

  @Test
  void isConnectionsStateOpen_bothConnectionsAreOpen() {
    when(callerConnection.isOpen()).thenReturn(true);
    when(targetWebSocket.isInputClosed()).thenReturn(false);

    assertThat(connectionsPair.isConnectionsStateOpen(), equalTo(true));
  }

  @Test
  void isConnectionsStateOpen_onlyTargetWebSocketIsOpen() {
    when(callerConnection.isOpen()).thenReturn(false);
    when(targetWebSocket.isInputClosed()).thenReturn(false);

    assertThat(connectionsPair.isConnectionsStateOpen(), equalTo(false));
  }

  @Test
  void isConnectionsStateOpen_onlyCallerIsOpen() {
    lenient().when(callerConnection.isOpen()).thenReturn(true);
    when(targetWebSocket.isInputClosed()).thenReturn(true);

    assertThat(connectionsPair.isConnectionsStateOpen(), equalTo(false));
  }

  @Test
  void sendTextToLocalWebSocket_dataIsProvided() {
    String msg = "MSG";
    connectionsPair.sendTextToLocalWebSocket(msg);

    verify(targetWebSocket, times(1)).sendText(eq(msg), eq(true));
  }

  @Test
  void close_bothConnectionsAreOpen() throws IOException {
    when(callerConnection.isOpen()).thenReturn(true);
    when(targetWebSocket.isInputClosed()).thenReturn(false);

    connectionsPair.close();

    verify(callerConnection, times(1)).close();
    verify(targetWebSocket, times(1)).sendClose(eq(500), any());
  }

  @Test
  void close_onlyCallerConnectionIsOpen() throws IOException {
    when(callerConnection.isOpen()).thenReturn(true);
    when(targetWebSocket.isInputClosed()).thenReturn(true);

    connectionsPair.close();

    verify(callerConnection, times(1)).close();
    verify(targetWebSocket, times(0)).sendClose(eq(500), any());
  }

  @Test
  void close_onlyWebSocketConnectionIsOpen() throws IOException {
    when(callerConnection.isOpen()).thenReturn(false);
    when(targetWebSocket.isInputClosed()).thenReturn(false);

    connectionsPair.close();

    verify(callerConnection, times(0)).close();
    verify(targetWebSocket, times(1)).sendClose(eq(500), any());
  }
}
