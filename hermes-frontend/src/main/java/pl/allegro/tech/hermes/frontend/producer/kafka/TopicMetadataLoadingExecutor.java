package pl.allegro.tech.hermes.frontend.producer.kafka;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static pl.allegro.tech.hermes.frontend.producer.kafka.TopicMetadataLoader.MetadataLoadingResult;
import static pl.allegro.tech.hermes.frontend.producer.kafka.TopicMetadataLoader.Type;
import static pl.allegro.tech.hermes.frontend.utils.CompletableFuturesHelper.allComplete;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.frontend.cache.topic.TopicsCache;
import pl.allegro.tech.hermes.frontend.metric.CachedTopic;

class TopicMetadataLoadingExecutor {

  private static final Logger logger = LoggerFactory.getLogger(TopicMetadataLoadingExecutor.class);

  private final ScheduledExecutorService scheduler;
  private final RetryPolicy<MetadataLoadingResult> retryPolicy;
  private final TopicsCache topicsCache;

  TopicMetadataLoadingExecutor(
      TopicsCache topicsCache, int retryCount, Duration retryInterval, int threadPoolSize) {
    this.topicsCache = topicsCache;
    ThreadFactory threadFactory =
        new ThreadFactoryBuilder().setNameFormat("topic-metadata-loader-%d").build();
    this.scheduler = Executors.newScheduledThreadPool(threadPoolSize, threadFactory);
    this.retryPolicy =
        new RetryPolicy<MetadataLoadingResult>()
            .withMaxRetries(retryCount)
            .withDelay(retryInterval)
            .handleIf((resp, cause) -> resp.isFailure());
  }

  boolean execute(List<TopicMetadataLoader> loaders) {
    try {
      long start = System.currentTimeMillis();
      logger.info("Loading topic metadata");
      List<CachedTopic> topics = topicsCache.getTopics();
      List<MetadataLoadingResult> allResults = loadMetadataForTopics(topics, loaders);
      logger.info("Finished loading topic metadata in {}ms", System.currentTimeMillis() - start);
      logResultInfo(allResults);
      return allResults.stream().noneMatch(MetadataLoadingResult::isFailure);
    } catch (Exception e) {
      logger.error("An error occurred while refreshing topic metadata", e);
      return false;
    }
  }

  private List<MetadataLoadingResult> loadMetadataForTopics(
      List<CachedTopic> topics, List<TopicMetadataLoader> loaders) {
    List<CompletableFuture<MetadataLoadingResult>> completableFutures = new ArrayList<>();
    for (CachedTopic topic : topics) {
      for (TopicMetadataLoader loader : loaders) {
        completableFutures.add(loadTopicMetadata(topic, loader));
      }
    }
    return allComplete(completableFutures).join();
  }

  private CompletableFuture<MetadataLoadingResult> loadTopicMetadata(
      CachedTopic topic, TopicMetadataLoader loader) {
    return Failsafe.with(retryPolicy)
        .with(scheduler)
        .getStageAsync((context) -> completedFuture(loader.load(topic)));
  }

  private void logResultInfo(List<MetadataLoadingResult> allResults) {
    Map<String, List<MetadataLoadingResult>> resultsPerDatacenter =
        allResults.stream()
            .collect(Collectors.groupingBy(MetadataLoadingResult::datacenter, Collectors.toList()));

    for (Map.Entry<String, List<MetadataLoadingResult>> datacenterResults :
        resultsPerDatacenter.entrySet()) {
      Map<Type, List<MetadataLoadingResult>> groupedResults =
          getGroupedResults(datacenterResults.getValue());
      Optional<List<MetadataLoadingResult>> successes =
          Optional.ofNullable(groupedResults.get(Type.SUCCESS));
      Optional<List<MetadataLoadingResult>> failures =
          Optional.ofNullable(groupedResults.get(Type.FAILURE));

      logger.info(
          "Results of loading topic metadata from datacenter {}: successfully loaded {} topics, failed for {} topics{}",
          datacenterResults.getKey(),
          successes.map(List::size).orElse(0),
          failures.map(List::size).orElse(0),
          failures
              .map(results -> String.format("Failed topics: [%s].", topicsOfResults(results)))
              .orElse(""));
    }
  }

  private Map<Type, List<MetadataLoadingResult>> getGroupedResults(
      List<MetadataLoadingResult> allResults) {
    return allResults.stream()
        .collect(Collectors.groupingBy(MetadataLoadingResult::type, Collectors.toList()));
  }

  private String topicsOfResults(List<MetadataLoadingResult> results) {
    return results.stream()
        .map(MetadataLoadingResult::topicName)
        .map(TopicName::qualifiedName)
        .collect(Collectors.joining(", "));
  }

  void close() throws Exception {
    scheduler.shutdown();
    scheduler.awaitTermination(1, TimeUnit.SECONDS);
  }
}
