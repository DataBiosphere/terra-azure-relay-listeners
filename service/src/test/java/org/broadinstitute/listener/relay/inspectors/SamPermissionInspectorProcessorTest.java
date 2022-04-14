package org.broadinstitute.listener.relay.inspectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SamPermissionInspectorProcessorTest {

  private SamPermissionInspector inspector;

  @Mock private SamResourceClient samResourceClient;

  @BeforeEach
  void setUp() {
    inspector = new SamPermissionInspector(samResourceClient);
  }

  @Test
  void getToken() {
    var res1 = inspector.getToken("");
    assertThat(res1, equalTo(Optional.empty()));

    var res2 = inspector.getToken("fake;");
    assertThat(res2, equalTo(Optional.empty()));

    var res3 = inspector.getToken("LeoToken=asdfaf");
    assertThat(res3, equalTo(Optional.of("asdfaf")));

    var res4 = inspector.getToken("aldl;LeoToken=asdfaf");
    assertThat(res4, equalTo(Optional.of("asdfaf")));
  }
}
