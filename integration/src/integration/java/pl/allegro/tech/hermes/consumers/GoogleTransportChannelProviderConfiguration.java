package pl.allegro.tech.hermes.consumers;

import com.google.api.gax.grpc.GrpcTransportChannel;
import com.google.api.gax.rpc.FixedTransportChannelProvider;
import com.google.api.gax.rpc.TransportChannelProvider;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;

@Configuration
public class GoogleTransportChannelProviderConfiguration {

    @Bean
    @Primary
    @Profile("integration")
    public TransportChannelProvider integrationTransportChannelProvider(ConfigFactory configFactory) {
        final ManagedChannel channel = ManagedChannelBuilder.forTarget(
                        configFactory.getStringProperty(Configs.GOOGLE_PUBSUB_TRANSPORT_CHANNEL_PROVIDER_ADDRESS))
                .usePlaintext().build();
        return FixedTransportChannelProvider.create(GrpcTransportChannel.create(channel));
    }
}
