package org.broadinstitute.listener.relay.http;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.microsoft.azure.relay.HybridConnectionListener;
import org.broadinstitute.listener.relay.transport.TargetHostResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ListenerConnectionHandlerTest {
  private static final String TARGET_HOST = "http://localhost:8080/";

  @Mock private TargetHostResolver targetHostResolver;
  @Mock private HybridConnectionListener listener;

  private ListenerConnectionHandler listenerConnectionHandler;

  @BeforeEach
  void setUp() {
    when(targetHostResolver.resolveTargetHost()).thenReturn(TARGET_HOST);
  }

  @Test
  void receiveRelayedHttpRequests_handlerIsSet() {
    listenerConnectionHandler = new ListenerConnectionHandler(listener, targetHostResolver);

    listenerConnectionHandler.receiveRelayedHttpRequests().subscribe();

    verify(listener, times(1)).setRequestHandler(any());
  }

  @Test
  void openConnection_listenerConnectionOpens() {
    listenerConnectionHandler = new ListenerConnectionHandler(listener, targetHostResolver);

    listenerConnectionHandler.openConnection().subscribe();

    verify(listener, times(1)).openAsync();
  }

  @Test
  void closeConnection_listenerConnectionCloses() {
    listenerConnectionHandler = new ListenerConnectionHandler(listener, targetHostResolver);

    listenerConnectionHandler.closeConnection().subscribe();

    verify(listener, times(1)).closeAsync();
  }
}
