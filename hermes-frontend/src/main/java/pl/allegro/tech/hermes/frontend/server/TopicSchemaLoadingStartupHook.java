package pl.allegro.tech.hermes.frontend.server;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static pl.allegro.tech.hermes.frontend.server.SchemaLoadingResult.Type.FAILURE;
import static pl.allegro.tech.hermes.frontend.server.SchemaLoadingResult.Type.MISSING;
import static pl.allegro.tech.hermes.frontend.server.SchemaLoadingResult.Type.SUCCESS;
import static pl.allegro.tech.hermes.frontend.utils.CompletableFuturesHelper.allComplete;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.ContentType;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.frontend.cache.topic.TopicsCache;
import pl.allegro.tech.hermes.frontend.metric.CachedTopic;
import pl.allegro.tech.hermes.frontend.server.SchemaLoadingResult.Type;
import pl.allegro.tech.hermes.schema.SchemaRepository;

public class TopicSchemaLoadingStartupHook {

  private static final Logger logger = LoggerFactory.getLogger(TopicSchemaLoadingStartupHook.class);

  private final TopicsCache topicsCache;

  private final SchemaRepository schemaRepository;

  private final int retryCount;

  private final int threadPoolSize;

  private final boolean isTopicSchemaLoadingStartupHookEnabled;

  public TopicSchemaLoadingStartupHook(
      TopicsCache topicsCache,
      SchemaRepository schemaRepository,
      int retryCount,
      int threadPoolSize,
      boolean isTopicSchemaLoadingStartupHookEnabled) {
    this.topicsCache = topicsCache;
    this.schemaRepository = schemaRepository;
    this.retryCount = retryCount;
    this.threadPoolSize = threadPoolSize;
    this.isTopicSchemaLoadingStartupHookEnabled = isTopicSchemaLoadingStartupHookEnabled;
  }

  public void run() {
    if (isTopicSchemaLoadingStartupHookEnabled) {
      long start = System.currentTimeMillis();
      logger.info("Loading topic schemas");
      List<Topic> topics = getAvroTopics();
      List<SchemaLoadingResult> allResults = loadSchemasForTopics(topics);
      logResultInfo(allResults, System.currentTimeMillis() - start);
    } else {
      logger.info("Loading topic schemas is disabled");
    }
  }

  private List<Topic> getAvroTopics() {
    return topicsCache.getTopics().stream()
        .map(CachedTopic::getTopic)
        .filter(topic -> ContentType.AVRO == topic.getContentType())
        .collect(toList());
  }

  private List<SchemaLoadingResult> loadSchemasForTopics(List<Topic> topics) {
    try (TopicSchemaLoader loader =
        new TopicSchemaLoader(schemaRepository, retryCount, threadPoolSize)) {
      return allComplete(topics.stream().map(loader::loadTopicSchema).collect(toList())).join();
    } catch (Exception e) {
      logger.error("An error occurred while loading schema topics", e);
      return Collections.emptyList();
    }
  }

  private void logResultInfo(List<SchemaLoadingResult> allResults, long elapsed) {
    Map<Type, List<SchemaLoadingResult>> groupedResults = getGroupedResults(allResults);
    Optional<List<SchemaLoadingResult>> successes =
        Optional.ofNullable(groupedResults.get(SUCCESS));
    Optional<List<SchemaLoadingResult>> missing = Optional.ofNullable(groupedResults.get(MISSING));
    Optional<List<SchemaLoadingResult>> failures = Optional.ofNullable(groupedResults.get(FAILURE));

    logger.info(
        "Finished loading schemas for {} topics in {}ms [successful: {}, missing: {}, failed: {}]. {}{}",
        allResults.size(),
        elapsed,
        successes.map(List::size).orElse(0),
        missing.map(List::size).orElse(0),
        failures.map(List::size).orElse(0),
        missing
            .map(results -> String.format("Missing schema for: [%s]", topicsOfResults(results)))
            .orElse(""),
        failures
            .map(results -> String.format("Failed for: [%s]. ", topicsOfResults(results)))
            .orElse(""));
  }

  private String topicsOfResults(List<SchemaLoadingResult> results) {
    return results.stream()
        .map(SchemaLoadingResult::getTopic)
        .map(Topic::getQualifiedName)
        .collect(joining(", "));
  }

  private Map<Type, List<SchemaLoadingResult>> getGroupedResults(
      List<SchemaLoadingResult> allResults) {
    return allResults.stream()
        .collect(Collectors.groupingBy(SchemaLoadingResult::getType, Collectors.toList()));
  }
}
