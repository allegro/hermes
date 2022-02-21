package pl.allegro.tech.hermes.consumers.consumer.sender.pubsub;

import com.google.api.gax.batching.BatchingSettings;
import com.google.api.gax.core.ExecutorProvider;
import com.google.api.gax.core.FixedExecutorProvider;
import com.google.api.gax.retrying.RetrySettings;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.threeten.bp.Duration;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSender;
import pl.allegro.tech.hermes.consumers.consumer.sender.ProtocolMessageSenderProvider;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class PubSubMessageSenderProvider implements ProtocolMessageSenderProvider {

    public static final String SUPPORTED_PROTOCOL = "pubsub";

    private static final Logger logger = LoggerFactory.getLogger(PubSubMessageSenderProvider.class);

    private final PubSubSenderTargetResolver resolver;
    private final ConfigFactory configFactory;
    private final PubSubCredentialsProvider credentialsProvider;
    private final PubSubMessages messageCreator;
    private final ConcurrentMap<Subscription, Publisher> publishersCache = new ConcurrentHashMap<>();
    private final ExecutorProvider publishingExecutorProvider;

    private final RetrySettings retrySettings;
    private final BatchingSettings batchingSettings;

    public PubSubMessageSenderProvider(PubSubSenderTargetResolver resolver,
                                       ConfigFactory configFactory,
                                       PubSubCredentialsProvider credentialsProvider,
                                       PubSubMessages messageCreator) {
        this.resolver = resolver;
        this.configFactory = configFactory;
        this.credentialsProvider = credentialsProvider;
        this.messageCreator = messageCreator;
        this.publishingExecutorProvider = FixedExecutorProvider.create(
                Executors.newScheduledThreadPool(configFactory.getIntProperty(Configs.PUBSUB_SENDER_CORE_POOL_SIZE),
                        new ThreadFactoryBuilder().setNameFormat("pubsub-publisher-%d").build()));
        this.retrySettings = retrySettings();
        this.batchingSettings = batchingSettings();
    }

    @Override
    public MessageSender create(Subscription subscription) {
        final Publisher publisher = publishersCache.computeIfAbsent(subscription, s -> {
            final PubSubSenderTarget resolvedTarget = resolver.resolve(s.getEndpoint());
            return createPublisher(resolvedTarget);
        });
        Preconditions.checkNotNull(publisher, "PubSub Publisher cannot be null");

        return new PubSubMessageSender(new PubSubClient(publisher, messageCreator));
    }

    @Override
    public Set<String> getSupportedProtocols() {
        return ImmutableSet.of(SUPPORTED_PROTOCOL);
    }

    @Override
    public void start() throws Exception {
    }

    @Override
    public void stop() throws Exception {
        // TODO check that method, because it looks like it is never called across the Hermes codebase
        publishersCache.values().parallelStream().forEach(p -> {
            p.shutdown();
            try {
                p.awaitTermination(30, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                logger.error("Interrupted termination of the PubSub publisher, it may result in messages loss");
            }
        });
        publishingExecutorProvider.getExecutor().shutdown();
    }

    private Publisher createPublisher(PubSubSenderTarget resolvedTarget) {
        try {
            return Publisher.newBuilder(resolvedTarget.getTopicName())
                    .setEndpoint(resolvedTarget.getPubSubEndpoint())
                    .setCredentialsProvider(credentialsProvider.getProvider())
                    .setRetrySettings(retrySettings)
                    .setBatchingSettings(batchingSettings)
                    .setExecutorProvider(publishingExecutorProvider)
                    .build();
        } catch (IOException e) {
            logger.error("Cannot instantiate PubSub Publisher", e);
            return null;
        }
    }

    private BatchingSettings batchingSettings() {
        long requestBytesThreshold = configFactory.getLongProperty(Configs.PUBSUB_SENDER_REQUEST_BYTES_THRESHOLD);
        long messageCountBatchSize = configFactory.getLongProperty(Configs.PUBSUB_SENDER_MESSAGE_COUNT_BATCH_SIZE);
        Duration publishDelayThreshold = Duration.ofMillis(
                configFactory.getLongProperty(Configs.PUBSUB_SENDER_PUBLISH_DELAY_THRESHOLD));

        return BatchingSettings.newBuilder()
                .setElementCountThreshold(messageCountBatchSize)
                .setRequestByteThreshold(requestBytesThreshold)
                .setDelayThreshold(publishDelayThreshold)
                .build();
    }

    private RetrySettings retrySettings() {
        Duration retryDelay = Duration.ofMillis(configFactory.getLongProperty(Configs.PUBSUB_SENDER_RETRY_DELAY));
        double retryDelayMultiplier = configFactory.getDoubleProperty(Configs.PUBSUB_SENDER_RETRY_DELAY_MULTIPLIER);
        Duration maxRetryDelay = Duration.ofMillis(
                configFactory.getLongProperty(Configs.PUBSUB_SENDER_MAX_RETRY_DELAY));
        Duration initialRpcTimeout = Duration.ofMillis(
                configFactory.getLongProperty(Configs.PUBSUB_SENDER_INITIAL_RPC_TIMEOUT));
        double rpcTimeoutMultiplier = configFactory.getDoubleProperty(Configs.PUBSUB_SENDER_RPC_TIMEOUT_MULTIPLIER);
        Duration maxRpcTimeout = Duration.ofMillis(
                configFactory.getLongProperty(Configs.PUBSUB_SENDER_MAX_RPC_TIMEOUT));
        Duration totalTimeout = Duration.ofMillis(
                configFactory.getLongProperty(Configs.PUBSUB_SENDER_TOTAL_TIMEOUT));

        return RetrySettings.newBuilder()
                .setInitialRetryDelay(retryDelay)
                .setRetryDelayMultiplier(retryDelayMultiplier)
                .setMaxRetryDelay(maxRetryDelay)
                .setInitialRpcTimeout(initialRpcTimeout)
                .setRpcTimeoutMultiplier(rpcTimeoutMultiplier)
                .setMaxRpcTimeout(maxRpcTimeout)
                .setTotalTimeout(totalTimeout)
                .build();
    }
}
