package pl.allegro.tech.hermes.frontend.server;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import net.jodah.failsafe.ExecutionContext;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.frontend.metric.CachedTopic;
import pl.allegro.tech.hermes.frontend.producer.BrokerMessageProducer;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.CompletableFuture.completedFuture;

class TopicMetadataLoader implements AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(TopicMetadataLoader.class);

    private final BrokerMessageProducer brokerMessageProducer;

    private final ScheduledExecutorService scheduler;

    private final RetryPolicy<MetadataLoadingResult> retryPolicy;

    TopicMetadataLoader(BrokerMessageProducer brokerMessageProducer,
                               int retryCount,
                               Duration retryInterval,
                               int threadPoolSize) {

        this.brokerMessageProducer = brokerMessageProducer;
        ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat("topic-metadata-loader-%d").build();
        this.scheduler = Executors.newScheduledThreadPool(threadPoolSize, threadFactory);
        this.retryPolicy = new RetryPolicy<MetadataLoadingResult>()
                .withMaxRetries(retryCount)
                .withDelay(retryInterval)
                .handleIf((resp, cause) -> resp.isFailure());
    }

    CompletableFuture<MetadataLoadingResult> loadTopicMetadata(CachedTopic topic) {
        return Failsafe.with(retryPolicy).with(scheduler)
                .getStageAsync((context) -> completedFuture(fetchTopicMetadata(topic, context)));
    }

    private MetadataLoadingResult fetchTopicMetadata(CachedTopic topic, ExecutionContext context) {
        int attempt = context.getAttemptCount();
        if (brokerMessageProducer.isTopicAvailable(topic)) {
            return MetadataLoadingResult.success(topic.getTopicName());
        }
        logger.warn("Failed to load metadata for topic {}, attempt #{}", topic.getQualifiedName(), attempt);
        return MetadataLoadingResult.failure(topic.getTopicName());
    }

    @Override
    public void close() throws Exception {
        scheduler.shutdown();
        scheduler.awaitTermination(1, TimeUnit.SECONDS);
    }
}
