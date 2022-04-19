package org.broadinstitute.listener.config;

import com.microsoft.azure.relay.HybridConnectionListener;
import com.microsoft.azure.relay.RelayConnectionStringBuilder;
import com.microsoft.azure.relay.TokenProvider;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import org.broadinstitute.dsde.workbench.client.sam.ApiClient;
import org.broadinstitute.listener.relay.http.RelayedHttpRequestProcessor;
import org.broadinstitute.listener.relay.inspectors.InspectorLocator;
import org.broadinstitute.listener.relay.inspectors.InspectorsProcessor;
import org.broadinstitute.listener.relay.inspectors.RequestInspector;
import org.broadinstitute.listener.relay.inspectors.SamResourceClient;
import org.broadinstitute.listener.relay.inspectors.TokenChecker;
import org.broadinstitute.listener.relay.transport.DefaultTargetResolver;
import org.broadinstitute.listener.relay.transport.TargetResolver;
import org.springframework.beans.factory.annotation.Autowired;
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
  public SamResourceClient samResourceClient() {
    ApiClient samClient = new ApiClient();
    samClient.setBasePath(properties.getSamInspectorProperties().samUrl());
    // return a simple resolver that uses the configuration value.
    return new SamResourceClient(
        properties.getSamInspectorProperties().samResourceId(),
        samClient,
        new TokenChecker());
  }

  @Bean
  public RelayedHttpRequestProcessor relayedHttpRequestProcessor(TargetResolver targetResolver) {
    return new RelayedHttpRequestProcessor(targetResolver);
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
