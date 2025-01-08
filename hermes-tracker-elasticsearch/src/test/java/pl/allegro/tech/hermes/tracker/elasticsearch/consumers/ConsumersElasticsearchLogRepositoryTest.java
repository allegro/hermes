package pl.allegro.tech.hermes.tracker.elasticsearch.consumers;

import static org.awaitility.Awaitility.await;
import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.termQuery;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import pl.allegro.tech.hermes.api.SentMessageTraceStatus;
import pl.allegro.tech.hermes.common.metric.MetricsFacade;
import pl.allegro.tech.hermes.tracker.consumers.AbstractLogRepositoryTest;
import pl.allegro.tech.hermes.tracker.consumers.LogRepository;
import pl.allegro.tech.hermes.tracker.elasticsearch.ElasticsearchResource;
import pl.allegro.tech.hermes.tracker.elasticsearch.LogSchemaAware;
import pl.allegro.tech.hermes.tracker.elasticsearch.SchemaManager;
import pl.allegro.tech.hermes.tracker.elasticsearch.frontend.FrontendDailyIndexFactory;
import pl.allegro.tech.hermes.tracker.elasticsearch.frontend.FrontendIndexFactory;

public class ConsumersElasticsearchLogRepositoryTest extends AbstractLogRepositoryTest
    implements LogSchemaAware {

  private static final String CLUSTER_NAME = "primary";

  private static final Clock clock =
      Clock.fixed(LocalDate.now().atStartOfDay().toInstant(ZoneOffset.UTC), ZoneId.systemDefault());
  private static final ConsumersIndexFactory indexFactory = new ConsumersDailyIndexFactory(clock);
  private static final FrontendIndexFactory frontendIndexFactory =
      new FrontendDailyIndexFactory(clock);
  private static final MetricsFacade metricsFacade = new MetricsFacade(new SimpleMeterRegistry());

  private static final ElasticsearchResource elasticsearch = new ElasticsearchResource();
  private static SchemaManager schemaManager;

  @BeforeClass
  public static void beforeAll() throws Throwable {
    elasticsearch.before();
    schemaManager =
        new SchemaManager(elasticsearch.client(), frontendIndexFactory, indexFactory, false);
  }

  @AfterClass
  public static void afterAll() {
    elasticsearch.after();
  }

  @Override
  protected LogRepository createLogRepository() {
    schemaManager.ensureSchema();
    return new ConsumersElasticsearchLogRepository.Builder(elasticsearch.client(), metricsFacade)
        .withIndexFactory(indexFactory)
        .build();
  }

  @Override
  protected void awaitUntilMessageIsPersisted(
      String topic, String subscription, String id, SentMessageTraceStatus status) {
    awaitUntilPersisted(getMessageFilter(topic, subscription, id, status));
  }

  @Override
  protected void awaitUntilBatchMessageIsPersisted(
      String topic,
      String subscription,
      String messageId,
      String batchId,
      SentMessageTraceStatus status) {
    awaitUntilPersisted(getMessageBatchFilter(topic, subscription, messageId, batchId, status));
  }

  private void awaitUntilPersisted(QueryBuilder query) {
    await()
        .atMost(Duration.ofMinutes(1))
        .until(
            () -> {
              SearchResponse response =
                  elasticsearch
                      .client()
                      .prepareSearch(indexFactory.createIndex())
                      .setTypes(SchemaManager.SENT_TYPE)
                      .setQuery(query)
                      .execute()
                      .get();
              return response.getHits().getTotalHits().value == 1;
            });
  }

  private BoolQueryBuilder getMessageFilter(
      String topic, String subscription, String id, SentMessageTraceStatus status) {
    BoolQueryBuilder queryBuilder =
        boolQuery()
            .must(termQuery(TOPIC_NAME, topic))
            .must(termQuery(SUBSCRIPTION, subscription))
            .must(termQuery(MESSAGE_ID, id))
            .must(termQuery(STATUS, status.toString()))
            .must(termQuery(CLUSTER, CLUSTER_NAME));
    return queryBuilder;
  }

  private QueryBuilder getMessageBatchFilter(
      String topic,
      String subscription,
      String messageId,
      String batchId,
      SentMessageTraceStatus status) {
    return getMessageFilter(topic, subscription, messageId, status)
        .must(termQuery(BATCH_ID, batchId));
  }
}
