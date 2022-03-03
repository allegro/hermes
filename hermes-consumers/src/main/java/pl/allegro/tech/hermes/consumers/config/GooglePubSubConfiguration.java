package pl.allegro.tech.hermes.consumers.config;

import com.google.api.gax.batching.BatchingSettings;
import com.google.api.gax.core.ExecutorProvider;
import com.google.api.gax.core.FixedExecutorProvider;
import com.google.api.gax.retrying.RetrySettings;
import com.google.common.cache.CacheBuilder;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.threeten.bp.Duration;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.consumers.consumer.sender.googlepubsub.GooglePubSubMessages;
import pl.allegro.tech.hermes.consumers.consumer.sender.googlepubsub.GooglePubSubMetadataAppender;
import pl.allegro.tech.hermes.consumers.consumer.sender.googlepubsub.GooglePubSubSenderTargetResolver;
import pl.allegro.tech.hermes.consumers.consumer.sender.googlepubsub.auth.GooglePubSubCredentialsProvider;
import pl.allegro.tech.hermes.consumers.consumer.sender.googlepubsub.cache.GooglePubSubPublishersCache;
import pl.allegro.tech.hermes.consumers.consumer.sender.googlepubsub.cache.GooglePubSubPublishersCacheLoader;

import javax.inject.Named;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Configuration
@ConditionalOnBean(GooglePubSubCredentialsProvider.class)
public class GooglePubSubConfiguration {

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
    public ScheduledExecutorService googlePubSubPublishingExecutor(ConfigFactory configFactory) {
        return Executors.newScheduledThreadPool(configFactory.getIntProperty(Configs.GOOGLE_PUBSUB_SENDER_CORE_POOL_SIZE),
                new ThreadFactoryBuilder().setNameFormat("pubsub-publisher-%d").build());
    }

    @Bean
    public ExecutorProvider googlePubSubPublishingExecutorProvider(
            @Named("googlePubSubPublishingExecutor") ScheduledExecutorService googlePubSubPublishingExecutor) {

        return FixedExecutorProvider.create(googlePubSubPublishingExecutor);
    }

    @Bean
    public GooglePubSubPublishersCacheLoader googlePubSubPublishersCacheLoader(
            GooglePubSubCredentialsProvider credentialsProvider,
            ExecutorProvider executorProvider,
            RetrySettings retrySettings,
            BatchingSettings batchingSettings) throws IOException {

        return new GooglePubSubPublishersCacheLoader(credentialsProvider.getProvider(), executorProvider, retrySettings,
                batchingSettings);
    }

    @Bean(name = "googlePubSubPublishersCache")
    public GooglePubSubPublishersCache googlePubSubPublishersCache(GooglePubSubPublishersCacheLoader cacheLoader, ConfigFactory configFactory) {
        return new GooglePubSubPublishersCache(
                CacheBuilder.newBuilder()
                        .maximumSize(configFactory.getLongProperty(Configs.GOOGLE_PUBSUB_PUBLISHERS_CACHE_SIZE))
                        .expireAfterAccess(configFactory.getLongProperty(Configs.GOOGLE_PUBSUB_PUBLISHERS_CACHE_EXPIRE_AFTER_ACCESS_SECONDS), TimeUnit.SECONDS)
                        .build(cacheLoader)
        );
    }

    @Bean
    public BatchingSettings batchingSettings(ConfigFactory configFactory) {
        long requestBytesThreshold = configFactory.getLongProperty(Configs.GOOGLE_PUBSUB_SENDER_REQUEST_BYTES_THRESHOLD);
        long messageCountBatchSize = configFactory.getLongProperty(Configs.GOOGLE_PUBSUB_SENDER_MESSAGE_COUNT_BATCH_SIZE);
        Duration publishDelayThreshold = Duration.ofMillis(configFactory.getLongProperty(Configs.GOOGLE_PUBSUB_SENDER_PUBLISH_DELAY_THRESHOLD));

        return BatchingSettings.newBuilder()
                .setElementCountThreshold(messageCountBatchSize)
                .setRequestByteThreshold(requestBytesThreshold)
                .setDelayThreshold(publishDelayThreshold)
                .build();
    }

    @Bean
    public RetrySettings retrySettings(ConfigFactory configFactory) {
        Duration totalTimeout = Duration.ofMillis(
                configFactory.getLongProperty(Configs.GOOGLE_PUBSUB_SENDER_TOTAL_TIMEOUT));

        return RetrySettings.newBuilder()
                .setInitialRpcTimeout(totalTimeout)
                .setMaxRpcTimeout(totalTimeout)
                .setTotalTimeout(totalTimeout)
                .setMaxAttempts(1)
                .build();
    }

}
