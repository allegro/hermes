package pl.allegro.tech.hermes.consumers;

import com.google.api.gax.grpc.GrpcTransportChannel;
import com.google.api.gax.rpc.FixedTransportChannelProvider;
import com.google.api.gax.rpc.TransportChannelProvider;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import pl.allegro.tech.hermes.consumers.config.GooglePubSubSenderProperties;

@Configuration
@EnableConfigurationProperties(GooglePubSubSenderProperties.class)
public class GoogleTransportChannelProviderConfiguration {

  @Bean
  @Primary
  @Profile("integration")
  public TransportChannelProvider integrationTransportChannelProvider(
      GooglePubSubSenderProperties googlePubSubSenderProperties) {
    final ManagedChannel channel =
        ManagedChannelBuilder.forTarget(
                googlePubSubSenderProperties.getTransportChannelProviderAddress())
            .usePlaintext()
            .build();
    return FixedTransportChannelProvider.create(GrpcTransportChannel.create(channel));
  }
}
