package org.broadinstitute.listener.config;

import com.microsoft.azure.relay.HybridConnectionListener;
import com.microsoft.azure.relay.RelayConnectionStringBuilder;
import com.microsoft.azure.relay.TokenProvider;
import java.net.URI;
import java.net.URISyntaxException;
import org.broadinstitute.listener.relay.transport.DefaultTargetResolver;
import org.broadinstitute.listener.relay.transport.TargetResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ListenerProperties.class)
public class AppConfiguration {

  @Autowired private ListenerProperties properties;

  @Bean
  TargetResolver targetResolver() {
    // return a simple resolver that uses the configuration value.
    return new DefaultTargetResolver(properties);
  }

  @Bean
  HybridConnectionListener listener() throws URISyntaxException {
    RelayConnectionStringBuilder connectionParams =
        new RelayConnectionStringBuilder(properties.getRelayConnectionString());
    TokenProvider tokenProvider =
        TokenProvider.createSharedAccessSignatureTokenProvider(
            connectionParams.getSharedAccessKeyName(), connectionParams.getSharedAccessKey());
    return new HybridConnectionListener(
        new URI(connectionParams.getEndpoint().toString() + connectionParams.getEntityPath()),
        tokenProvider);
  }
}
