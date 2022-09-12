package org.broadinstitute.listener.relay.transport;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.net.URISyntaxException;
import java.net.URL;
import java.util.stream.Stream;
import org.apache.http.client.utils.URIBuilder;
import org.broadinstitute.listener.relay.InvalidRelayTargetException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class TargetURIParserTest {

  private TargetURIParser targetURIParser;
  private static final String TARGET_HOST = "https://localhost:8080";

  @ParameterizedTest
  @MethodSource("parseScenarios")
  void parseTargetHttpUrl_scenarioWithTargetHostWithoutEndingSlash(
      String relayedURI, String segmentsToRemove, String expectedTarget)
      throws InvalidRelayTargetException, URISyntaxException {
    URIBuilder builder = new URIBuilder(relayedURI);
    targetURIParser = new TargetURIParser(TARGET_HOST, builder.build());
    URL result = targetURIParser.parseTargetHttpUrl(segmentsToRemove);

    assertThat(result.toString(), equalTo(expectedTarget));
  }

  @ParameterizedTest
  @MethodSource("parseScenarios")
  void parseTargetHttpUrl_scenarioWithTargetHostWithEndingSlash(
      String relayedURI, String segmentsToRemove, String expectedTarget)
      throws InvalidRelayTargetException, URISyntaxException {
    URIBuilder builder = new URIBuilder(relayedURI);
    targetURIParser = new TargetURIParser(TARGET_HOST + "/", builder.build());
    URL result = targetURIParser.parseTargetHttpUrl(segmentsToRemove);

    assertThat(result.toString(), equalTo(expectedTarget));
  }

  private static Stream<Arguments> parseScenarios() {
    return Stream.of(
        Arguments.of("https://r/c/p1/p2?q=1", "", TARGET_HOST + "/c/p1/p2?q=1"),
        Arguments.of("https://r/c/p1/p2?q=1", "p1/p3", TARGET_HOST + "/c/p1/p2?q=1"),
        Arguments.of("https://r/c/p1/p2?q=1", "c/p2", TARGET_HOST + "/c/p1/p2?q=1"),
        Arguments.of("https://r/c/p1/p2?q=1", "c/p1", TARGET_HOST + "/p2?q=1"),
        Arguments.of("https://r/c/p1/p2?q=1", "p2", TARGET_HOST + "/c/p1?q=1"),
        Arguments.of("https://r/c/p1/p1?q=1", "p1", TARGET_HOST + "/c?q=1"),
        Arguments.of("https://r/c/p1/p2?q=1", "c", TARGET_HOST + "/p1/p2?q=1"),
        Arguments.of("https://r/c/p1/p2?q=1", "/c", TARGET_HOST + "/p1/p2?q=1"),
        Arguments.of("https://r/c/p1/p2?q=1", "c/", TARGET_HOST + "/p1/p2?q=1"),
        Arguments.of("https://r/c/p1/p2?q=1", "p1", TARGET_HOST + "/c/p2?q=1"),
        Arguments.of("https://r/c/p1/p2?q=1", "p1/", TARGET_HOST + "/c/p2?q=1"),
        Arguments.of("https://r/c/p1/p2?q=1", "/p1/", TARGET_HOST + "/c/p2?q=1"),
        Arguments.of("https://r/c/p1/p2?q=1", "p1/p2", TARGET_HOST + "/c?q=1"),
        Arguments.of("https://r/c/p1/p2?q=1", "p1/p2/", TARGET_HOST + "/c?q=1"),
        Arguments.of("https://r/c/p1/p2?q=1", "/p1/p2/", TARGET_HOST + "/c?q=1"),
        Arguments.of("https://r/c/p1/p2?q=1", "c/p1/p2/", TARGET_HOST + "/?q=1"),
        Arguments.of("https://r/c/p1/p2?q=1", "c/p1/p2", TARGET_HOST + "/?q=1"),
        Arguments.of("https://r/c/$hc/p1/p2?q=1", "c", TARGET_HOST + "/p1/p2?q=1"),
        Arguments.of("https://r/c/$hc/p1/p2?q=1", "", TARGET_HOST + "/c/p1/p2?q=1"));
  }
}
