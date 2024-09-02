package pl.allegro.tech.hermes.frontend.server;

import static java.util.concurrent.CompletableFuture.completedFuture;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import net.jodah.failsafe.ExecutionContext;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.schema.CouldNotLoadSchemaException;
import pl.allegro.tech.hermes.schema.SchemaNotFoundException;
import pl.allegro.tech.hermes.schema.SchemaRepository;

class TopicSchemaLoader implements AutoCloseable {

  private static final Logger logger = LoggerFactory.getLogger(TopicSchemaLoader.class);

  private final SchemaRepository schemaRepository;

  private final ScheduledExecutorService scheduler;

  private final RetryPolicy<SchemaLoadingResult> retryPolicy;

  TopicSchemaLoader(SchemaRepository schemaRepository, int retryCount, int threadPoolSize) {
    this.schemaRepository = schemaRepository;

    ThreadFactory threadFactory =
        new ThreadFactoryBuilder().setNameFormat("topic-schema-loader-%d").build();
    this.scheduler = Executors.newScheduledThreadPool(threadPoolSize, threadFactory);
    this.retryPolicy =
        new RetryPolicy<SchemaLoadingResult>()
            .withMaxRetries(retryCount)
            .handleIf((resp, cause) -> resp.isFailure());
  }

  CompletableFuture<SchemaLoadingResult> loadTopicSchema(Topic topic) {
    return Failsafe.with(retryPolicy)
        .with(scheduler)
        .getStageAsync((context) -> completedFuture(loadLatestSchema(topic, context)));
  }

  private SchemaLoadingResult loadLatestSchema(Topic topic, ExecutionContext context) {
    int attempt = context.getAttemptCount();
    try {
      schemaRepository.getLatestAvroSchema(topic);
      logger.info(
          "Successfully loaded schema for topic {}, attempt #{}",
          topic.getQualifiedName(),
          attempt);
      return SchemaLoadingResult.success(topic);
    } catch (SchemaNotFoundException e) {
      logger.warn(
          "Failed to load schema for topic {}, attempt #{}. {}",
          topic.getQualifiedName(),
          attempt,
          e.getMessage());
      return SchemaLoadingResult.missing(topic);
    } catch (CouldNotLoadSchemaException e) {
      logger.error(
          "Failed to load schema for topic {}, attempt #{}", topic.getQualifiedName(), attempt, e);
    }
    return SchemaLoadingResult.failure(topic);
  }

  @Override
  public void close() throws Exception {
    scheduler.shutdown();
    scheduler.awaitTermination(1, TimeUnit.SECONDS);
  }
}
