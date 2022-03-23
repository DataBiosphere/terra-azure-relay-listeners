package org.broadinstitute.listener.relay.inspectors;

import com.microsoft.azure.relay.RelayedHttpListenerRequest;

public interface RequestInspector {

  public boolean inspectWebSocketUpgradeRequest(
      RelayedHttpListenerRequest relayedHttpListenerRequest);

  public boolean inspectRelayedHttpRequest(RelayedHttpListenerRequest relayedHttpListenerRequest);
}
