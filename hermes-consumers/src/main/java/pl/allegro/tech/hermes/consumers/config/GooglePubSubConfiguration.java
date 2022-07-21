package pl.allegro.tech.hermes.consumers.config;

import com.google.api.gax.batching.BatchingSettings;
import com.google.api.gax.core.ExecutorProvider;
import com.google.api.gax.core.FixedExecutorProvider;
import com.google.api.gax.retrying.RetrySettings;
import com.google.api.gax.rpc.TransportChannelProvider;
import com.google.cloud.pubsub.v1.TopicAdminSettings;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.threeten.bp.Duration;
import pl.allegro.tech.hermes.consumers.consumer.sender.googlepubsub.GooglePubSubMessages;
import pl.allegro.tech.hermes.consumers.consumer.sender.googlepubsub.GooglePubSubMetadataAppender;
import pl.allegro.tech.hermes.consumers.consumer.sender.googlepubsub.GooglePubSubSenderTargetResolver;

import javax.inject.Named;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Configuration
@EnableConfigurationProperties({
        GooglePubSubSenderProperties.class
})
public class GooglePubSubConfiguration {

    @Bean
    public TransportChannelProvider transportChannelProvider() {
        return TopicAdminSettings.defaultGrpcTransportProviderBuilder().setChannelsPerCpu(1).build();
    }

    @Bean
    public GooglePubSubMessages pubSubMessages(GooglePubSubMetadataAppender googlePubSubMetadataAppender) {
        return new GooglePubSubMessages(googlePubSubMetadataAppender);
    }

    @Bean
    public GooglePubSubMetadataAppender pubSubMetadataAppender() {
        return new GooglePubSubMetadataAppender();
    }

    @Bean
    public GooglePubSubSenderTargetResolver pubSubSenderTargetResolver() {
        return new GooglePubSubSenderTargetResolver();
    }

    @Bean(name = "googlePubSubPublishingExecutor", destroyMethod = "shutdown")
    public ScheduledExecutorService googlePubSubPublishingExecutor(GooglePubSubSenderProperties googlePubSubSenderProperties) {
        return Executors.newScheduledThreadPool(googlePubSubSenderProperties.getCorePoolSize(),
                new ThreadFactoryBuilder().setNameFormat("pubsub-publisher-%d").build());
    }

    @Bean
    public ExecutorProvider googlePubSubPublishingExecutorProvider(
            @Named("googlePubSubPublishingExecutor") ScheduledExecutorService googlePubSubPublishingExecutor) {

        return FixedExecutorProvider.create(googlePubSubPublishingExecutor);
    }

    @Bean
    public BatchingSettings batchingSettings(GooglePubSubSenderProperties googlePubSubSenderProperties) {
        long requestBytesThreshold = googlePubSubSenderProperties.getBatchingRequestBytesThreshold();
        long messageCountBatchSize = googlePubSubSenderProperties.getBatchingMessageCountBytesSize();
        Duration publishDelayThreshold = Duration.ofMillis(googlePubSubSenderProperties.getBatchingPublishDelayThreshold().toMillis());

        return BatchingSettings.newBuilder()
                .setElementCountThreshold(messageCountBatchSize)
                .setRequestByteThreshold(requestBytesThreshold)
                .setDelayThreshold(publishDelayThreshold)
                .build();
    }

    @Bean
    public RetrySettings retrySettings(GooglePubSubSenderProperties googlePubSubSenderProperties) {
        Duration totalTimeout = Duration.ofMillis(googlePubSubSenderProperties.getTotalTimeout().toMillis());

        return RetrySettings.newBuilder()
                .setInitialRpcTimeout(totalTimeout)
                .setMaxRpcTimeout(totalTimeout)
                .setTotalTimeout(totalTimeout)
                .setMaxAttempts(1)
                .build();
    }
}
