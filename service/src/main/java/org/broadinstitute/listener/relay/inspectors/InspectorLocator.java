package org.broadinstitute.listener.relay.inspectors;

import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class InspectorLocator {

  public final ApplicationContext context;

  public InspectorLocator(ApplicationContext context) {
    this.context = context;
  }

  public RequestInspector getInspector(InspectorType inspectorType) {
    Object bean = context.getBean(inspectorType.getInspectorName());

    return (RequestInspector) bean;
  }
}
