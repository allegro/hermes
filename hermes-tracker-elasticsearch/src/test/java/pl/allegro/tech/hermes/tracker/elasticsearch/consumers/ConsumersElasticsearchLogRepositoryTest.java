package pl.allegro.tech.hermes.tracker.elasticsearch.consumers;

import com.codahale.metrics.MetricRegistry;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import pl.allegro.tech.hermes.api.SentMessageTraceStatus;
import pl.allegro.tech.hermes.metrics.PathsCompiler;
import pl.allegro.tech.hermes.tracker.consumers.AbstractLogRepositoryTest;
import pl.allegro.tech.hermes.tracker.consumers.LogRepository;
import pl.allegro.tech.hermes.tracker.elasticsearch.ElasticsearchResource;
import pl.allegro.tech.hermes.tracker.elasticsearch.LogSchemaAware;
import pl.allegro.tech.hermes.tracker.elasticsearch.SchemaManager;
import pl.allegro.tech.hermes.tracker.elasticsearch.frontend.FrontendDailyIndexFactory;
import pl.allegro.tech.hermes.tracker.elasticsearch.frontend.FrontendIndexFactory;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static com.jayway.awaitility.Awaitility.await;
import static com.jayway.awaitility.Duration.ONE_MINUTE;
import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.termQuery;

public class ConsumersElasticsearchLogRepositoryTest extends AbstractLogRepositoryTest implements LogSchemaAware {

    private static final String CLUSTER_NAME = "primary";

    private static final Clock clock = Clock.fixed(LocalDate.now().atStartOfDay().toInstant(ZoneOffset.UTC), ZoneId.systemDefault());
    private static final ConsumersIndexFactory indexFactory = new ConsumersDailyIndexFactory(clock);
    private static final FrontendIndexFactory frontendIndexFactory = new FrontendDailyIndexFactory(clock);

    private static ElasticsearchResource elasticsearch = new ElasticsearchResource();
    private SchemaManager schemaManager;

    @BeforeSuite
    public void before() throws Throwable {
        elasticsearch.before();
        schemaManager = new SchemaManager(elasticsearch.client(), frontendIndexFactory, indexFactory, false);
    }

    @AfterSuite
    public void after() {
        elasticsearch.after();
    }

    @Override
    protected LogRepository createLogRepository() {
        schemaManager.ensureSchema();
        return new ConsumersElasticsearchLogRepository.Builder(elasticsearch.client(), new PathsCompiler("localhost"), new MetricRegistry())
                .withIndexFactory(indexFactory)
                .build();
    }

    @Override
    protected void awaitUntilMessageIsPersisted(String topic, String subscription, String id,
                                                SentMessageTraceStatus status) {
        awaitUntilPersisted(getMessageFilter(topic, subscription, id, status));
    }

    @Override
    protected void awaitUntilBatchMessageIsPersisted(String topic, String subscription, String messageId, String batchId,
                                                     SentMessageTraceStatus status) {
        awaitUntilPersisted(getMessageBatchFilter(topic, subscription, messageId, batchId, status));
    }

    private void awaitUntilPersisted(QueryBuilder query) {
        await().atMost(ONE_MINUTE).until(() -> {
            SearchResponse response = elasticsearch.client().prepareSearch(indexFactory.createIndex())
                    .setTypes(SchemaManager.SENT_TYPE)
                    .setQuery(query)
                    .execute().get();
            return response.getHits().getTotalHits() == 1;
        });
    }

    private BoolQueryBuilder getMessageFilter(String topic, String subscription, String id,
                                              SentMessageTraceStatus status) {
        BoolQueryBuilder queryBuilder = boolQuery()
                .must(termQuery(TOPIC_NAME, topic))
                .must(termQuery(SUBSCRIPTION, subscription))
                .must(termQuery(MESSAGE_ID, id))
                .must(termQuery(STATUS, status.toString()))
                .must(termQuery(CLUSTER, CLUSTER_NAME));
        return queryBuilder;
    }

    private QueryBuilder getMessageBatchFilter(String topic, String subscription, String messageId, String batchId,
                                               SentMessageTraceStatus status) {
        return getMessageFilter(topic, subscription, messageId, status)
                .must(termQuery(BATCH_ID, batchId));
    }
}
