package org.broadinstitute.listener.relay.inspectors;

public enum InspectorType {
  HEADERS_LOGGER(InspectorNameConstants.HEADERS_LOGGER),
  SET_DATE_ACCESSED(InspectorNameConstants.SET_DATE_ACCESSED),
  SAM_CHECKER(InspectorNameConstants.SAM_CHECKER);

  private final String inspectorName;

  InspectorType(String inspectorName) {
    this.inspectorName = inspectorName;
  }

  public String getInspectorName() {
    return inspectorName;
  }

  public interface InspectorNameConstants {
    String HEADERS_LOGGER = "headersLogger";
    String SAM_CHECKER = "samChecker";
    String SET_DATE_ACCESSED = "setDateAccessed";
  }
}
