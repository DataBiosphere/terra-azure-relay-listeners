package org.broadinstitute.listener.relay.inspectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.microsoft.azure.relay.RelayedHttpListenerRequest;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InspectorsProcessorTest {

  private InspectorsProcessor inspectorsProcessor;
  @Mock private RelayedHttpListenerRequest listenerRequest;
  @Mock private RequestInspector inspector1;
  @Mock private RequestInspector inspector2;

  @BeforeEach
  void setUp() {
    inspectorsProcessor = new InspectorsProcessor(List.of(inspector1, inspector2));
  }

  @ParameterizedTest
  @MethodSource("provideInspectorScenarios")
  void isRelayedWebSocketUpgradeRequestAccepted_scenarioIsProvided(
      boolean firstInspector, boolean secondInspector, boolean expectedResult) {
    when(inspector1.inspectWebSocketUpgradeRequest(any())).thenReturn(firstInspector);
    when(inspector2.inspectWebSocketUpgradeRequest(any())).thenReturn(secondInspector);

    boolean result = inspectorsProcessor.isRelayedWebSocketUpgradeRequestAccepted(listenerRequest);

    assertThat(result, equalTo(expectedResult));
  }

  private static Stream<Arguments> provideInspectorScenarios() {
    return Stream.of(
        Arguments.of(true, true, true),
        Arguments.of(true, false, false),
        Arguments.of(false, true, false),
        Arguments.of(false, false, false));
  }

  @ParameterizedTest
  @MethodSource("provideInspectorScenarios")
  void isRelayedHttpRequestAccepted_scenarioIsProvided(
      boolean firstInspector, boolean secondInspector, boolean expectedResult) {
    when(inspector1.inspectRelayedHttpRequest(any())).thenReturn(firstInspector);
    when(inspector2.inspectRelayedHttpRequest(any())).thenReturn(secondInspector);

    boolean result = inspectorsProcessor.isRelayedHttpRequestAccepted(listenerRequest);

    assertThat(result, equalTo(expectedResult));
  }
}
