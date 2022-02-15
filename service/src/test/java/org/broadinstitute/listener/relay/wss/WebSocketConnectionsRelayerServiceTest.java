package org.broadinstitute.listener.relay.wss;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WebSocketConnectionsRelayerServiceTest {

  @Mock private ConnectionsPair connectionsPair;
  @Captor private ArgumentCaptor<String> wsMsg;
  private WebSocketConnectionsRelayerService relayerService;
  private static final String WS_MSG = "{hello world}";

  @BeforeEach
  void setUp() {
    relayerService = new WebSocketConnectionsRelayerService();
  }

  @AfterEach
  void tearDown() {
    relayerService.stopService();
  }

  @Test
  void readAndSendText_dataIsReadAndSent() {
    when(connectionsPair.readTextFromCaller()).thenReturn(WS_MSG);

    relayerService.readAndSendText(connectionsPair);
    verify(connectionsPair, times(1)).readTextFromCaller();
    verify(connectionsPair, times(1)).sendTextToLocalWebSocket(wsMsg.capture());

    assertThat(wsMsg.getValue(), equalTo(WS_MSG));
  }

  @Test
  void relayDataToLocalEndpoint_dataIsReadAndSentUntilConnectionCloses() {
    when(connectionsPair.isConnectionsStateOpen()).thenReturn(true).thenReturn(false);
    when(connectionsPair.readTextFromCaller()).thenReturn(WS_MSG);

    relayerService.relayDataToLocalEndpoint(connectionsPair);

    verify(connectionsPair, times(1)).readTextFromCaller();
    verify(connectionsPair, times(1)).sendTextToLocalWebSocket(wsMsg.capture());

    assertThat(wsMsg.getValue(), equalTo(WS_MSG));
  }
}
