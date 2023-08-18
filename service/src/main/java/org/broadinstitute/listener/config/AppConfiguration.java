package org.broadinstitute.listener.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.relay.HybridConnectionListener;
import com.microsoft.azure.relay.RelayConnectionStringBuilder;
import com.microsoft.azure.relay.TokenProvider;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Version;
import java.util.ArrayList;
import java.util.List;
import org.broadinstitute.listener.relay.http.RelayedHttpRequestProcessor;
import org.broadinstitute.listener.relay.inspectors.InspectorLocator;
import org.broadinstitute.listener.relay.inspectors.InspectorsProcessor;
import org.broadinstitute.listener.relay.inspectors.RequestInspector;
import org.broadinstitute.listener.relay.inspectors.SamResourceClient;
import org.broadinstitute.listener.relay.inspectors.SetDateAccessedInspectorOptions;
import org.broadinstitute.listener.relay.inspectors.TokenChecker;
import org.broadinstitute.listener.relay.transport.DefaultTargetResolver;
import org.broadinstitute.listener.relay.transport.TargetResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ListenerProperties.class)
@EnableCaching
public class AppConfiguration {
  @Autowired private ListenerProperties properties;

  @Bean
  public TargetResolver targetResolver() {
    // return a simple resolver that uses the configuration value.
    return new DefaultTargetResolver(properties);
  }

  @Bean
  public SamResourceClient samResourceClient(TokenChecker tokenChecker) {
    return new SamResourceClient(
        properties.getSetDateAccessedInspectorProperties().workspaceId(),
        properties.getSamInspectorProperties().samUrl(),
        properties.getSamInspectorProperties().samResourceId(),
        properties.getSamInspectorProperties().samResourceType(),
        tokenChecker,
        properties.getSamInspectorProperties().samAction());
  }

  @Bean
  public SetDateAccessedInspectorOptions setDateAccessedInspectorOptions() {
    return new SetDateAccessedInspectorOptions(
        properties.getSetDateAccessedInspectorProperties().serviceHost(),
        properties.getSetDateAccessedInspectorProperties().workspaceId(),
        properties.getSetDateAccessedInspectorProperties().callWindowInSeconds(),
        properties.getSetDateAccessedInspectorProperties().runtimeName(),
        HttpClient.newBuilder().version(Version.HTTP_1_1).build());
  }

  @Bean
  public RelayedHttpRequestProcessor relayedHttpRequestProcessor(
      TargetResolver targetResolver,
      TokenChecker tokenChecker,
      HealthEndpoint healthEndpoint,
      ObjectMapper objectMapper,
      SamResourceClient samResourceClient) {
    return new RelayedHttpRequestProcessor(
        targetResolver,
        properties.getCorsSupportProperties(),
        tokenChecker,
        healthEndpoint,
        objectMapper,
        samResourceClient);
  }

  @Bean
  public HybridConnectionListener listener() throws URISyntaxException {
    RelayConnectionStringBuilder connectionParams =
        new RelayConnectionStringBuilder(properties.getRelayConnectionString());
    TokenProvider tokenProvider =
        TokenProvider.createSharedAccessSignatureTokenProvider(
            connectionParams.getSharedAccessKeyName(), connectionParams.getSharedAccessKey());
    return new HybridConnectionListener(
        new URI(connectionParams.getEndpoint().toString() + connectionParams.getEntityPath()),
        tokenProvider);
  }

  @Bean
  public InspectorsProcessor inspectorsProcessor(InspectorLocator inspectorLocator) {
    List<RequestInspector> inspectors = new ArrayList<>();

    if (properties.getRequestInspectors() != null) {
      properties
          .getRequestInspectors()
          .forEach(i -> inspectors.add(inspectorLocator.getInspector(i)));
    }

    return new InspectorsProcessor(inspectors);
  }
}
