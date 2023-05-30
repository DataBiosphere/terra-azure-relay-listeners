package org.broadinstitute.listener.relay.inspectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.broadinstitute.listener.relay.Utils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UtilsTest {

  @Test
  void getToken() {
    var res1 = Utils.getToken(new HashMap<>());
    assertThat(res1, equalTo(Optional.empty()));

    Map<String, String> headers2 = new HashMap<>();
    headers2.put("cookie", "fake;");
    var res2 = Utils.getToken(headers2);
    assertThat(res2, equalTo(Optional.empty()));

    Map<String, String> headers3 = new HashMap<>();
    headers3.put("cookie", "LeoToken=asdfaf");
    var res3 = Utils.getToken(headers3);
    assertThat(res3, equalTo(Optional.of("asdfaf")));

    Map<String, String> headers4 = new HashMap<>();
    headers4.put("cookie", "aldl;LeoToken=asdfaf");
    var res4 = Utils.getToken(headers4);
    assertThat(res4, equalTo(Optional.of("asdfaf")));

    Map<String, String> headers5 = new HashMap<>();
    headers5.put("Authorization", "Bearer asdf");
    var res5 = Utils.getToken(headers5);
    assertThat(res5, equalTo(Optional.of("asdf")));

    Map<String, String> headers6 = new HashMap<>();
    headers6.put("Cookie", "LeoToken=asdfaf");
    var res6 = Utils.getToken(headers3);
    assertThat(res6, equalTo(Optional.of("asdfaf")));
  }
}
