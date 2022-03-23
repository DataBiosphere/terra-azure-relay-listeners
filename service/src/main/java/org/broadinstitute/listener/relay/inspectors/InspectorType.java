package org.broadinstitute.listener.relay.inspectors;

public enum InspectorType {
  HEADERS_LOGGER(InspectorNameConstants.HEADERS_LOGGER);

  private final String inspectorName;

  InspectorType(String inspectorName) {
    this.inspectorName = inspectorName;
  }

  public String getInspectorName() {
    return inspectorName;
  }

  public interface InspectorNameConstants {
    String HEADERS_LOGGER = "headersLogger";
  }
}
