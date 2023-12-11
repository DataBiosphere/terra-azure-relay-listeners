package org.broadinstitute.listener.relay.transport;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.http.client.utils.URIBuilder;
import org.broadinstitute.listener.config.ListenerProperties;
import org.broadinstitute.listener.config.TargetProperties;
import org.broadinstitute.listener.config.TargetRoutingRule;
import org.broadinstitute.listener.relay.InvalidRelayTargetException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.util.UriUtils;

class DefaultTargetResolverTest {

  private static final String TARGET_HOST = "localhost:8080";

  private static final String RULE_TARGET_HOST = "localhost:8081";
  private static final String RULE_TARGET_URL = "http://localhost:8081";

  private static final String RULE_CONTAINS = "mzt";

  private static final String TARGET_HOST_HTTPS = "https://localhost:8080/";

  private static final String RELAY_HOST = "tom.foo.com";
  private static final String HYBRID_CONN = "connection";
  private static final String TARGET_PATH = "data1";
  private static final String TARGET_PATH_WITH_SPACE = "data 1";
  private static final String TARGET_QS = "var1=foo&var2=bar";
  private static final String RELAY_REQUEST =
      RELAY_HOST + "/" + HYBRID_CONN + TARGET_PATH + TARGET_QS;
  private static final String EXPECTED_TARGET_URL =
      String.format("http://%s/%s/%s?%s", TARGET_HOST, HYBRID_CONN, TARGET_PATH, TARGET_QS);
  private static final String EXPECTED_TARGET_HOST_HTTPS =
      TARGET_HOST_HTTPS + HYBRID_CONN + TARGET_PATH + TARGET_QS;

  private ListenerProperties properties;
  private DefaultTargetResolver resolver;

  @BeforeEach
  void setUp() {
    properties = new ListenerProperties();
    properties.setTargetProperties(new TargetProperties());
    properties.getTargetProperties().setTargetHost("http://" + TARGET_HOST);
    properties.setRelayConnectionName(HYBRID_CONN);

    resolver = new DefaultTargetResolver(properties);
  }

  @Test
  void createTargetWebSocketUri_requestWithPathAndQueryNoEntityPath()
      throws URISyntaxException, InvalidRelayTargetException {
    URI relayRequest = createRelayRequest(TARGET_PATH, TARGET_QS, false);

    properties.getTargetProperties().setRemoveEntityPathFromWssUri(true);
    URI target = resolver.createTargetWebSocketUri(relayRequest);

    assertThat(target.toString(), equalTo(getExpectedTargetWsUri(TARGET_PATH)));
  }

  @Test
  void createTargetWebSocketUri_requestWithPathAndQueryWithEntityPath()
      throws URISyntaxException, InvalidRelayTargetException {
    URI relayRequest = createRelayRequest(TARGET_PATH, TARGET_QS, false);

    properties.getTargetProperties().setRemoveEntityPathFromWssUri(false);
    URI target = resolver.createTargetWebSocketUri(relayRequest);

    assertThat(target.toString(), equalTo(getExpectedTargetWsUri(HYBRID_CONN + "/" + TARGET_PATH)));
  }

  @Test
  void createTargetUrl_requestWithPathAndQueryAndEntityPath()
      throws URISyntaxException, InvalidRelayTargetException {
    URI relayRequest = createRelayRequest(TARGET_PATH, TARGET_QS, false);

    URL target = resolver.createTargetUrl(relayRequest);

    assertThat(target.toString(), equalTo(getExpectedTargetUrl(TARGET_PATH)));
    assertThat(target.toString().contains(HYBRID_CONN), equalTo(true));
  }

  @Test
  void createTargetUrl_requestThatMatchesRuleAndRuleHasHCWildCard_returnsRuleUrlWithoutHC()
      throws URISyntaxException, InvalidRelayTargetException {
    URI relayRequest = createRelayRequest(RULE_CONTAINS, TARGET_QS, false);

    properties
        .getTargetProperties()
        .setTargetRoutingRules(
            List.of(
                new TargetRoutingRule(
                    RULE_CONTAINS, RULE_TARGET_URL, "$hc-name/" + RULE_CONTAINS)));

    URL target = resolver.createTargetUrl(relayRequest);

    assertThat(target.toString(), equalTo(String.format("%s/?%s", RULE_TARGET_URL, TARGET_QS)));
  }

  @Test
  void createTargetUrl_requestThatMatchesRule_returnsRuleUrl()
      throws URISyntaxException, InvalidRelayTargetException {
    URI relayRequest = createRelayRequest(RULE_CONTAINS, TARGET_QS, false);

    properties
        .getTargetProperties()
        .setTargetRoutingRules(List.of(new TargetRoutingRule(RULE_CONTAINS, RULE_TARGET_URL, "")));

    URL target = resolver.createTargetUrl(relayRequest);

    assertThat(target.toString(), equalTo(getExpectedRuleTargetUrl(RULE_CONTAINS)));
    assertThat(target.toString().contains(HYBRID_CONN), equalTo(true));
  }

  @Test
  void createTargetUrl_requestDoesNotMatchRule_returnsDefaultUrl()
      throws URISyntaxException, InvalidRelayTargetException {
    URI relayRequest = createRelayRequest(RULE_CONTAINS, TARGET_QS, false);

    properties
        .getTargetProperties()
        .setTargetRoutingRules(
            List.of(new TargetRoutingRule(RULE_CONTAINS + "invalid", RULE_TARGET_URL, "")));

    URL target = resolver.createTargetUrl(relayRequest);

    assertThat(target.toString(), equalTo(getExpectedTargetUrl(RULE_CONTAINS)));
    assertThat(target.toString().contains(HYBRID_CONN), equalTo(true));
  }

  @Test
  void createTargetUrl_requestWithPathAndQueryAndNoEntityPath()
      throws URISyntaxException, InvalidRelayTargetException {
    URI relayRequest = createRelayRequest(TARGET_PATH, TARGET_QS, false);

    properties.getTargetProperties().setRemoveEntityPathFromHttpUrl(true);

    URL target = resolver.createTargetUrl(relayRequest);

    assertThat(target.toString().contains(HYBRID_CONN), equalTo(false));
  }

  @Test
  void createTargetUrl_requestWithSpaceInPathAndQuery()
      throws URISyntaxException, InvalidRelayTargetException {
    URI relayRequest = createRelayRequest(TARGET_PATH_WITH_SPACE, TARGET_QS, false);

    URL target = resolver.createTargetUrl(relayRequest);

    assertThat(
        target.toString(),
        equalTo(getExpectedTargetUrl(UriUtils.encodePath(TARGET_PATH_WITH_SPACE, "UTF-8"))));
  }

  @Test
  void createTargetHeaders_authorization() {
    var relayHeaders =
        Map.of(
            "Content-Type",
            "application/json",
            "Authorization",
            "Bearer abcd",
            "Content-Length",
            "42");
    var targetHeaders = resolver.createTargetHeaders(relayHeaders);

    assertThat(targetHeaders, equalTo(relayHeaders));
  }

  @Test
  void createTargetHeaders_removeAuthorization() {
    var properties = new ListenerProperties();
    properties.setTargetProperties(new TargetProperties());
    properties.getTargetProperties().setTargetHost("http://" + TARGET_HOST);
    properties.getTargetProperties().setRemoveAuthorizationHeader(true);
    properties.setRelayConnectionName(HYBRID_CONN);
    var resolver = new DefaultTargetResolver(properties);

    var relayHeaders =
        Map.of(
            "Content-Type",
            "application/json",
            "Authorization",
            "Bearer abcd",
            "Content-Length",
            "42");
    var targetHeaders = resolver.createTargetHeaders(relayHeaders);

    assertThat(
        targetHeaders, equalTo(Map.of("Content-Type", "application/json", "Content-Length", "42")));
  }

  @Test
  void createTargetHeaders_null() {
    var targetHeaders = resolver.createTargetHeaders(null);

    assertThat(targetHeaders, nullValue());
  }

  private URI createRelayRequest(String path, String query, boolean addWSSegment)
      throws URISyntaxException {

    List<String> segments = new ArrayList<>();

    if (addWSSegment) {
      segments.add("$hc");
    }

    segments.add(HYBRID_CONN);
    segments.add(path);

    URIBuilder builder =
        new URIBuilder()
            .setScheme("https")
            .setHost(RELAY_HOST)
            .setPathSegments(segments)
            .setCustomQuery(query);

    return builder.build();
  }

  private String getExpectedTargetUrl(String path) {
    return String.format("http://%s/%s/%s?%s", TARGET_HOST, HYBRID_CONN, path, TARGET_QS);
  }

  private String getExpectedRuleTargetUrl(String path) {
    return String.format("http://%s/%s/%s?%s", RULE_TARGET_HOST, HYBRID_CONN, path, TARGET_QS);
  }

  private String getExpectedTargetWsUri(String path) {
    return String.format("ws://%s/%s?%s", TARGET_HOST, path, TARGET_QS);
  }
}
