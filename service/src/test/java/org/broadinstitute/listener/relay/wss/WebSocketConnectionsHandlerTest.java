package org.broadinstitute.listener.relay.wss;

import static org.junit.jupiter.api.Assertions.*;

import com.microsoft.azure.relay.HybridConnectionListener;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

class WebSocketConnectionsHandlerTest {

  @Mock private HybridConnectionListener listener;

  @BeforeEach
  void setUp() {}

  @Test
  void acceptHttpUpgradeRequests() {}

  @Test
  void acceptConnections() {}

  @Test
  void createLocalConnection() {}
}
