package org.broadinstitute.listener.relay;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

public record OauthInfo(Optional<Instant> expiresAt, String error, Map<String, String> claims) {}
