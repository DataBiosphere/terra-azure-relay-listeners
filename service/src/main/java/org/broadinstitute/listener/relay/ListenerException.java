package org.broadinstitute.listener.relay;

public class ListenerException extends Exception {

  public ListenerException(String message, Throwable cause) {
    super(message, cause);
  }

  public ListenerException(String message) {
    super(message);
  }
}
