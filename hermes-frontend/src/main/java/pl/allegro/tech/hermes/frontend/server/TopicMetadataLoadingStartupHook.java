package pl.allegro.tech.hermes.frontend.server;

import org.glassfish.hk2.api.ServiceLocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.common.hook.Hook;
import pl.allegro.tech.hermes.common.hook.ServiceAwareHook;
import pl.allegro.tech.hermes.frontend.cache.topic.TopicsCache;
import pl.allegro.tech.hermes.frontend.metric.CachedTopic;
import pl.allegro.tech.hermes.frontend.producer.BrokerMessageProducer;

import javax.inject.Inject;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;
import static pl.allegro.tech.hermes.common.config.Configs.FRONTEND_STARTUP_TOPIC_METADATA_LOADING_THREAD_POOL_SIZE;
import static pl.allegro.tech.hermes.frontend.server.CompletableFuturesHelper.allComplete;
import static pl.allegro.tech.hermes.frontend.server.MetadataLoadingResult.Type.FAILURE;
import static pl.allegro.tech.hermes.frontend.server.MetadataLoadingResult.Type.SUCCESS;

public class TopicMetadataLoadingStartupHook implements ServiceAwareHook {

    private static final Logger logger = LoggerFactory.getLogger(TopicMetadataLoadingStartupHook.class);

    private final BrokerMessageProducer brokerMessageProducer;

    private final TopicsCache topicsCache;

    private final int retryCount;

    private final long retryInterval;

    private final int threadPoolSize;

    @Inject
    public TopicMetadataLoadingStartupHook(BrokerMessageProducer brokerMessageProducer,
                                           TopicsCache topicsCache,
                                           ConfigFactory config) {
        this(brokerMessageProducer,
                topicsCache,
                config.getIntProperty(Configs.FRONTEND_STARTUP_TOPIC_METADATA_LOADING_RETRY_COUNT),
                config.getLongProperty(Configs.FRONTEND_STARTUP_TOPIC_METADATA_LOADING_RETRY_INTERVAL),
                config.getIntProperty(FRONTEND_STARTUP_TOPIC_METADATA_LOADING_THREAD_POOL_SIZE));
    }

    TopicMetadataLoadingStartupHook(BrokerMessageProducer brokerMessageProducer,
                                           TopicsCache topicsCache,
                                           int retryCount,
                                           long retryInterval,
                                           int threadPoolSize) {
        this.brokerMessageProducer = brokerMessageProducer;
        this.topicsCache = topicsCache;
        this.retryCount = retryCount;
        this.retryInterval = retryInterval;
        this.threadPoolSize = threadPoolSize;
    }

    @Override
    public void accept(ServiceLocator serviceLocator) {
        long start = System.currentTimeMillis();
        logger.info("Loading topics metadata");
        List<CachedTopic> topics = topicsCache.getTopics();
        List<MetadataLoadingResult> allResults = loadMetadataForTopics(topics);
        logResultInfo(allResults, System.currentTimeMillis() - start);
    }

    private List<MetadataLoadingResult> loadMetadataForTopics(List<CachedTopic> topics) {
        try (TopicMetadataLoader loader = new TopicMetadataLoader(brokerMessageProducer, retryCount, retryInterval, threadPoolSize)) {
            return allComplete(topics.stream().map(loader::loadTopicMetadata).collect(toList())).join();
        } catch (Exception e) {
            logger.error("An error occurred while loading topic metadata", e);
            return Collections.emptyList();
        }
    }

    private void logResultInfo(List<MetadataLoadingResult> allResults, long elapsed) {
        Map<MetadataLoadingResult.Type, List<MetadataLoadingResult>> groupedResults = getGroupedResults(allResults);
        Optional<List<MetadataLoadingResult>> successes = Optional.ofNullable(groupedResults.get(SUCCESS));
        Optional<List<MetadataLoadingResult>> failures = Optional.ofNullable(groupedResults.get(FAILURE));

        logger.info("Finished loading metadata for {} topics in {}ms [successful: {}, failed: {}]. {}",
                allResults.size(), elapsed, successes.map(List::size).orElse(0), failures.map(List::size).orElse(0),
                failures.map(results -> String.format("Failed for: [%s].", topicsOfResults(results))).orElse(""));
    }

    private Map<MetadataLoadingResult.Type, List<MetadataLoadingResult>> getGroupedResults(List<MetadataLoadingResult> allResults) {
        return allResults.stream().collect(Collectors.groupingBy(MetadataLoadingResult::getType, Collectors.toList()));
    }

    private String topicsOfResults(List<MetadataLoadingResult> results) {
        return results.stream().map(MetadataLoadingResult::getTopicName).map(TopicName::qualifiedName)
                .collect(Collectors.joining(", "));
    }

    @Override
    public int getPriority() {
        return Hook.HIGHER_PRIORITY;
    }
}
