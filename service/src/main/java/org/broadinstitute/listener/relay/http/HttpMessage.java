package org.broadinstitute.listener.relay.http;

import java.io.InputStream;
import java.util.Map;
import java.util.Optional;

public abstract class HttpMessage {

  private final Map<String, String> headers;
  private final InputStream body;

  protected HttpMessage(Map<String, String> headers, InputStream body) {
  this.headers = headers;
  this.body = body;
  }

  public Optional<Map<String, String>> getHeaders() {
  return Optional.ofNullable(headers);
  }

  public Optional<InputStream> getBody() {
  return Optional.ofNullable(body);
  }
}
