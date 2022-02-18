package org.broadinstitute.listener.relay;

public class InvalidRelayTargetException extends Exception {

  public InvalidRelayTargetException(String message, Throwable cause) {
    super(message, cause);
  }

  public InvalidRelayTargetException(String message) {
    super(message);
  }
}
