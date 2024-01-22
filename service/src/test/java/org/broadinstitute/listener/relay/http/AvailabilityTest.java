package org.broadinstitute.listener.relay.http;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.microsoft.azure.relay.HybridConnectionListener;
import org.broadinstitute.listener.relay.health.HybridConnectionListenerHealth;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.availability.ApplicationAvailability;
import org.springframework.boot.availability.AvailabilityChangeEvent;
import org.springframework.boot.availability.LivenessState;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc
@SpringBootTest({"spring.main.allow-bean-definition-overriding=true"})
@ActiveProfiles("test")
class AvailabilityTest {

  @Autowired private MockMvc mvc;
  @Autowired private ApplicationContext context;
  @Autowired private ApplicationAvailability applicationAvailability;
  @Autowired private HybridConnectionListenerHealth hybridConnectionListenerHealth;

  @MockBean private HybridConnectionListener hybridConnectionListener;

  @BeforeEach
  void beforeEach() {
    when(hybridConnectionListener.isOnline()).thenReturn(true);
  }

  // Reference: https://www.baeldung.com/spring-liveness-readiness-probes
  @Test
  void readinessState() throws Exception {
    assertThat(
        "Readiness state should be ACCEPTING_TRAFFIC",
        applicationAvailability.getReadinessState(),
        equalTo(ReadinessState.ACCEPTING_TRAFFIC));
    ResultActions readinessResult = mvc.perform(get("/actuator/health/readiness"));
    readinessResult.andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"));

    AvailabilityChangeEvent.publish(context, ReadinessState.REFUSING_TRAFFIC);
    assertThat(
        "Readiness state should be REFUSING_TRAFFIC",
        applicationAvailability.getReadinessState(),
        equalTo(ReadinessState.REFUSING_TRAFFIC));
    mvc.perform(get("/actuator/health/readiness"))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.status").value("OUT_OF_SERVICE"));
  }

  @Test
  void livenessState() throws Exception {
    assertThat(
        "Liveness state should be CORRECT",
        applicationAvailability.getLivenessState(),
        equalTo(LivenessState.CORRECT));
    ResultActions result = mvc.perform(get("/actuator/health/liveness"));
    result.andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"));
    ResultActions livenessResult = mvc.perform(get("/actuator/health/liveness"));
    livenessResult.andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"));

    AvailabilityChangeEvent.publish(context, LivenessState.BROKEN);
    assertThat(
        "Liveness state should be BROKEN",
        applicationAvailability.getLivenessState(),
        equalTo(LivenessState.BROKEN));
    mvc.perform(get("/actuator/health/liveness"))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.status").value("DOWN"));
  }

  /**
   * Verify that the HybridConnectionListenerHealth custom health check contributes to the UP/DOWN
   * status of this app's liveness probe.
   *
   * @throws Exception on exception in the test
   */
  @Test
  void hybridConnectionListenerHealth() throws Exception {
    // livenessState and liveness probe should both be ok, to start
    assertThat(
        "Liveness state should be CORRECT",
        applicationAvailability.getLivenessState(),
        equalTo(LivenessState.CORRECT));
    ResultActions result = mvc.perform(get("/actuator/health/liveness"));
    result.andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"));
    ResultActions livenessResult = mvc.perform(get("/actuator/health/liveness"));
    livenessResult.andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"));

    // set the (mock) hybridConnectionListener to be offline
    when(hybridConnectionListener.isOnline()).thenReturn(false);
    // trigger a health check
    Health actualHealth = hybridConnectionListenerHealth.health();

    // health check should be DOWN since hybridConnectionListener is offline
    assertEquals(Status.DOWN, actualHealth.getStatus());

    // liveness probe overall status should be DOWN since hybridConnectionListener is offline
    mvc.perform(get("/actuator/health/liveness"))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.status").value("DOWN"));

    // livenessState should remain CORRECT. The HybridConnectionListenerHealth check affects
    // the liveness probe, but not the liveness state.
    // in other words, livenessProbe = livenessState && hybridConnectionListenerHealth.
    assertThat(
        "Liveness state should be CORRECT",
        applicationAvailability.getLivenessState(),
        equalTo(LivenessState.CORRECT));
  }
}
