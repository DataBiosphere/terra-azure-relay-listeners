package org.broadinstitute.listener.relay.http;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.microsoft.azure.relay.HybridConnectionListener;
import org.broadinstitute.listener.config.ListenerProperties;
import org.broadinstitute.listener.config.TargetProperties;
import org.broadinstitute.listener.relay.inspectors.InspectorsProcessor;
import org.broadinstitute.listener.relay.transport.DefaultTargetResolver;
import org.broadinstitute.listener.relay.transport.TargetResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ListenerConnectionHandlerTest {
  private static final String TARGET_HOST = "http://localhost:8080/";
  private TargetResolver targetResolver;
  @Mock private HybridConnectionListener listener;
  @Mock private InspectorsProcessor inspectorsProcessor;

  private ListenerConnectionHandler listenerConnectionHandler;

  @BeforeEach
  void setUp() {
    ListenerProperties properties = new ListenerProperties();
    properties.setTargetProperties(new TargetProperties());
    properties.getTargetProperties().setTargetHost(TARGET_HOST);
    targetResolver = new DefaultTargetResolver(properties);
  }

  @Test
  void receiveRelayedHttpRequests_handlerIsSet() {
    listenerConnectionHandler =
        new ListenerConnectionHandler(listener, targetResolver, inspectorsProcessor);

    listenerConnectionHandler.receiveRelayedHttpRequests().subscribe();

    verify(listener, times(1)).setRequestHandler(any());
  }

  @Test
  void openConnection_listenerConnectionOpens() {
    listenerConnectionHandler =
        new ListenerConnectionHandler(listener, targetResolver, inspectorsProcessor);

    listenerConnectionHandler.openConnection().subscribe();

    verify(listener, times(1)).openAsync();
  }

  @Test
  void closeConnection_listenerConnectionCloses() {
    listenerConnectionHandler =
        new ListenerConnectionHandler(listener, targetResolver, inspectorsProcessor);

    listenerConnectionHandler.closeConnection().subscribe();

    verify(listener, times(1)).closeAsync();
  }
}
