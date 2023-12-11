package org.broadinstitute.listener.relay.transport;

import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.listener.config.ListenerProperties;
import org.broadinstitute.listener.config.TargetRoutingRule;
import org.broadinstitute.listener.relay.InvalidRelayTargetException;
import org.springframework.lang.NonNull;

public class DefaultTargetResolver implements TargetResolver {
  public static final String HC_NAME_RULE_WILD_CARD = "$hc-name";
  private final String defaultTargetHost;
  private final ListenerProperties properties;

  public DefaultTargetResolver(ListenerProperties properties) {
    this.properties = properties;
    if (properties.getTargetProperties() == null
        || StringUtils.isBlank(properties.getTargetProperties().getTargetHost())) {
      throw new IllegalStateException("The target host configuration is missing.");
    }
    defaultTargetHost = properties.getTargetProperties().getTargetHost();
  }

  @Override
  public URI createTargetWebSocketUri(@NonNull URI relayedRequestUri)
      throws InvalidRelayTargetException {
    URL targetUrl =
        createTargetUrl(
            relayedRequestUri, properties.getTargetProperties().isRemoveEntityPathFromWssUri());

    if (targetUrl.getProtocol().equals("http")) {
      return URI.create(targetUrl.toString().replaceFirst("http://", "ws://"));
    }

    if (targetUrl.getProtocol().equals("https")) {
      return URI.create(targetUrl.toString().replaceFirst("https://", "wss://"));
    }

    throw new InvalidRelayTargetException(
        "Invalid target URL. The target must be an HTTP/HTTPS endpoint");
  }

  @Override
  public URL createTargetUrl(@NonNull URI relayedRequestUri) throws InvalidRelayTargetException {

    return createTargetUrl(
        relayedRequestUri, properties.getTargetProperties().isRemoveEntityPathFromHttpUrl());
  }

  @Override
  public Map<String, String> createTargetHeaders(Map<String, String> relayedHeaders) {
    Map<String, String> targetHeaders = null;

    if (relayedHeaders != null) {
      targetHeaders = new HashMap<>(relayedHeaders);
      if (properties.getTargetProperties().isRemoveAuthorizationHeader()) {
        targetHeaders.remove("Authorization");
      }
    }

    return targetHeaders;
  }

  private TargetRule resolveTargetRule(@NonNull URI relayedRequestUri, boolean removeEntityPath) {
    List<TargetRoutingRule> rules = properties.getTargetProperties().getTargetRoutingRules();
    if (rules == null || rules.size() == 0) {
      return getDefaultRule(removeEntityPath);
    }
    return rules.stream()
        .filter(r -> relayedRequestUri.toString().contains(r.pathContains()))
        .map(this::createTargetRule)
        .findFirst()
        .orElse(getDefaultRule(removeEntityPath));
  }

  private TargetRule createTargetRule(TargetRoutingRule configurationRule) {

    String segmentToRemove = replaceHybridConnWildCardWithConnectionName(configurationRule);

    return new TargetRule(configurationRule.targetHost(), segmentToRemove);
  }

  private String replaceHybridConnWildCardWithConnectionName(TargetRoutingRule configurationRule) {
    String segmentToRemove =
        configurationRule
            .removeFromPath()
            .replace(HC_NAME_RULE_WILD_CARD, properties.getRelayConnectionName());
    return segmentToRemove;
  }

  private TargetRule getDefaultRule(boolean removeEntityPath) {
    String segmentsToRemove = "";
    if (removeEntityPath) {
      segmentsToRemove = properties.getRelayConnectionName();
    }
    return new TargetRule(defaultTargetHost, segmentsToRemove);
  }

  private URL createTargetUrl(URI relayedRequestUri, boolean removeEntityPath)
      throws InvalidRelayTargetException {

    TargetRule rule = resolveTargetRule(relayedRequestUri, removeEntityPath);

    TargetURIParser parser = new TargetURIParser(rule.targetHost(), relayedRequestUri);

    return parser.parseTargetHttpUrl(rule.segmentsToRemove());
  }
}
