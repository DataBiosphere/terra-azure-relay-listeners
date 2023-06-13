package org.broadinstitute.listener.relay;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.broadinstitute.listener.config.CorsSupportProperties;
import org.junit.jupiter.api.Test;

class UtilTest {
  @Test
  void getTokenFromAuthorization_should_get_token_properly() {
    Map<String, String> headers1 = new HashMap<>();
    headers1.put("Authorization", "Bearer asdf");
    var res1 = Utils.getTokenFromAuthorization(headers1);
    assertThat(res1, equalTo(Optional.of("asdf")));

    Map<String, String> headers2 = new HashMap<>();
    headers2.put("Authorization", "Bearer1 asdf");
    var res2 = Utils.getTokenFromAuthorization(headers2);
    assertThat(res2, equalTo(Optional.empty()));
  }

  @Test
  void isSetCookiePath() {
    var uri =
        URI.create(
            "https://relay-ns-995014ac-a05d-477e-9a75-1aa10bd5def5.servicebus.windows.net/saturn-4c90d242-dc9d-4d26-b858-4b7c73a97efc/setCookie");
    var res1 = Utils.isSetCookiePath(uri);
    assertThat(res1, equalTo(true));

    var uri2 =
        URI.create(
            "https://relay-ns-995014ac-a05d-477e-9a75-1aa10bd5def5.servicebus.windows.net/setCookie");
    var res2 = Utils.isSetCookiePath(uri2);
    assertThat(res2, equalTo(false));

    var uri3 =
        URI.create(
            "https://relay-ns-995014ac-a05d-477e-9a75-1aa10bd5def5.servicebus.windows.net/saturn-4c90d242-dc9d-4d26-b858-4b7c73a97efc/setCookie/");
    var res3 = Utils.isSetCookiePath(uri3);
    assertThat(res3, equalTo(true));

    var uri4 =
        URI.create(
            "https://relay-ns-995014ac-a05d-477e-9a75-1aa10bd5def5.servicebus.windows.net/saturn-4c90d242-dc9d-4d26-b858-4b7c73a97efc/setcookie");
    var res4 = Utils.isSetCookiePath(uri4);
    assertThat(res4, equalTo(true));
  }

  @Test
  void isValidOrigin_test() {
    // The space is intentional. The strings from azure_vm_init_script come prepended with spaces.
    String allowedOrigin = " myorigin.com";

    CorsSupportProperties mockCorsSupportProperties =
        new CorsSupportProperties("", "", "", "", List.of(allowedOrigin));

    String originToTest = "myorigin.com";
    boolean isValidOrigin = Utils.isValidOrigin(originToTest, mockCorsSupportProperties);
    assert (isValidOrigin);

    String correctOriginWithHttps = "https://" + "myorigin.com";
    isValidOrigin = Utils.isValidOrigin(correctOriginWithHttps, mockCorsSupportProperties);
    assert (isValidOrigin);
  }

  @Test
  void isValidOrigin_regex_test() {
    String allowAllOrigin = "*";
    CorsSupportProperties mockCorsSupportProperties =
        new CorsSupportProperties("", "", "", "", List.of(allowAllOrigin));

    String originToTest = "htttp://myorigin.csom";
    boolean isValidOrigin = Utils.isValidOrigin(originToTest, mockCorsSupportProperties);
    assert (isValidOrigin);

    String wildCardAllowedOrigin = "*.bee.envs.terra*";
    mockCorsSupportProperties =
        new CorsSupportProperties("", "", "", "", List.of(wildCardAllowedOrigin));

    isValidOrigin = Utils.isValidOrigin("nathan.bee.envs.terra", mockCorsSupportProperties);
    assert (isValidOrigin);
  }

  @Test
  void isValidOrigin_Error_test() {
    boolean isValidOrigin;
    String allowedOrigin = "myorigin.com";
    CorsSupportProperties mockCorsSupportProperties =
        new CorsSupportProperties("", "", "", "", List.of(allowedOrigin));

    String invalidOrigin = "notmyorigin.com";

    isValidOrigin = Utils.isValidOrigin(invalidOrigin, mockCorsSupportProperties);
    assert (!isValidOrigin);

    invalidOrigin = "myorigin.com.envs.bio";

    isValidOrigin = Utils.isValidOrigin(invalidOrigin, mockCorsSupportProperties);
    assert (!isValidOrigin);

    String validOriginWithPort = "myorigin.com:3000";
    isValidOrigin = Utils.isValidOrigin(validOriginWithPort, mockCorsSupportProperties);
    assert !isValidOrigin;
  }
}
