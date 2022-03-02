package pl.allegro.tech.hermes.consumers.consumer.sender.googlepubsub.cache;

import com.google.api.gax.batching.BatchingSettings;
import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.ExecutorProvider;
import com.google.api.gax.retrying.RetrySettings;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.common.cache.CacheLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.consumers.consumer.sender.googlepubsub.GooglePubSubSenderTarget;

import java.io.IOException;

public class GooglePubSubPublishersCacheLoader extends CacheLoader<GooglePubSubSenderTarget, Publisher> {

    private static final Logger logger = LoggerFactory.getLogger(GooglePubSubPublishersCacheLoader.class);

    private final CredentialsProvider credentialsProvider;
    private final ExecutorProvider publishingExecutorProvider;
    private final RetrySettings retrySettings;
    private final BatchingSettings batchingSettings;

    public GooglePubSubPublishersCacheLoader(CredentialsProvider credentialsProvider,
                                             ExecutorProvider publishingExecutorProvider,
                                             RetrySettings retrySettings,
                                             BatchingSettings batchingSettings) {
        this.credentialsProvider = credentialsProvider;
        this.publishingExecutorProvider = publishingExecutorProvider;
        this.retrySettings = retrySettings;
        this.batchingSettings = batchingSettings;
    }

    @Override
    public Publisher load(GooglePubSubSenderTarget resolvedTarget) throws Exception {
        try {
            return Publisher.newBuilder(resolvedTarget.getTopicName())
                    .setEndpoint(resolvedTarget.getPubSubEndpoint())
                    .setCredentialsProvider(credentialsProvider)
                    .setRetrySettings(retrySettings)
                    .setBatchingSettings(batchingSettings)
                    .setExecutorProvider(publishingExecutorProvider)
                    .build();
        } catch (IOException e) {
            logger.error("Cannot instantiate PubSub Publisher", e);
            return null;
        }
    }
}
