package pl.allegro.tech.hermes.frontend.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.frontend.cache.topic.TopicsCache;
import pl.allegro.tech.hermes.frontend.metric.CachedTopic;
import pl.allegro.tech.hermes.frontend.producer.BrokerMessageProducer;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;
import static pl.allegro.tech.hermes.frontend.server.CompletableFuturesHelper.allComplete;
import static pl.allegro.tech.hermes.frontend.server.MetadataLoadingResult.Type.FAILURE;
import static pl.allegro.tech.hermes.frontend.server.MetadataLoadingResult.Type.SUCCESS;

public class TopicMetadataLoadingRunner {

    private static final Logger logger = LoggerFactory.getLogger(TopicMetadataLoadingRunner.class);

    private final BrokerMessageProducer brokerMessageProducer;

    private final TopicsCache topicsCache;

    private final int retryCount;

    private final Duration retryInterval;

    private final int threadPoolSize;

    public TopicMetadataLoadingRunner(BrokerMessageProducer brokerMessageProducer,
                               TopicsCache topicsCache,
                               int retryCount,
                               Duration retryInterval,
                               int threadPoolSize) {
        this.brokerMessageProducer = brokerMessageProducer;
        this.topicsCache = topicsCache;
        this.retryCount = retryCount;
        this.retryInterval = retryInterval;
        this.threadPoolSize = threadPoolSize;
    }

    public List<MetadataLoadingResult> refreshMetadata() throws Exception {
        long start = System.currentTimeMillis();
        logger.info("Loading topics metadata");
        List<CachedTopic> topics = topicsCache.getTopics();
        List<MetadataLoadingResult> allResults = loadMetadataForTopics(topics);
        logResultInfo(allResults, System.currentTimeMillis() - start);
        return allResults;
    }

    private List<MetadataLoadingResult> loadMetadataForTopics(List<CachedTopic> topics) throws Exception {
        try (TopicMetadataLoader loader = new TopicMetadataLoader(brokerMessageProducer, retryCount, retryInterval, threadPoolSize)) {
            return allComplete(topics.stream().map(loader::loadTopicMetadata).collect(toList())).join();
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

}