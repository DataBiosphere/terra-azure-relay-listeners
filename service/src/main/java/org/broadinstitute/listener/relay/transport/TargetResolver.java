package org.broadinstitute.listener.relay.transport;

import java.net.URI;
import java.net.URL;
import org.broadinstitute.listener.relay.InvalidRelayTargetException;
import org.springframework.lang.NonNull;

public interface TargetResolver {

  URL createTargetUrl(@NonNull URI relayedRequestUri) throws InvalidRelayTargetException;

  URI createTargetWebSocketUri(@NonNull URI relayedRequestUri) throws InvalidRelayTargetException;
}
