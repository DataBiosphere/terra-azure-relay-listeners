package org.broadinstitute.listener.relay.http;

import java.io.InputStream;
import java.util.Map;
import java.util.Optional;

public abstract class HttpMessage {

  private final Optional<Map<String, String>> headers;
  private final Optional<InputStream> body;

  protected HttpMessage(Map<String, String> headers, InputStream body) {
    this.headers = Optional.ofNullable(headers);
    this.body = Optional.ofNullable(body);
  }

  public Optional<Map<String, String>> getHeaders() {
    return headers;
  }

  public Optional<InputStream> getBody() {
    return body;
  }
}
