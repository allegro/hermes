package pl.allegro.tech.hermes.tracker.elasticsearch.frontend;

import com.codahale.metrics.MetricRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import pl.allegro.tech.hermes.api.PublishedMessageTraceStatus;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.common.metric.MetricsFacade;
import pl.allegro.tech.hermes.metrics.PathsCompiler;
import pl.allegro.tech.hermes.tracker.elasticsearch.ElasticsearchResource;
import pl.allegro.tech.hermes.tracker.elasticsearch.LogSchemaAware;
import pl.allegro.tech.hermes.tracker.elasticsearch.SchemaManager;
import pl.allegro.tech.hermes.tracker.elasticsearch.consumers.ConsumersDailyIndexFactory;
import pl.allegro.tech.hermes.tracker.elasticsearch.consumers.ConsumersIndexFactory;
import pl.allegro.tech.hermes.tracker.frontend.AbstractLogRepositoryTest;
import pl.allegro.tech.hermes.tracker.frontend.LogRepository;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static com.jayway.awaitility.Awaitility.await;
import static com.jayway.awaitility.Duration.ONE_MINUTE;
import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.matchQuery;
import static org.elasticsearch.index.query.QueryBuilders.termQuery;

public class FrontendElasticsearchLogRepositoryTest extends AbstractLogRepositoryTest implements LogSchemaAware {

    private static final String CLUSTER_NAME = "primary";

    private static final Clock clock = Clock.fixed(LocalDate.now().atStartOfDay().toInstant(ZoneOffset.UTC), ZoneId.systemDefault());
    private static final FrontendIndexFactory frontendIndexFactory = new FrontendDailyIndexFactory(clock);
    private static final ConsumersIndexFactory consumersIndexFactory = new ConsumersDailyIndexFactory(clock);
    private static final MetricsFacade metricsFacade = new MetricsFacade(
            new SimpleMeterRegistry(),
            new HermesMetrics(new MetricRegistry(), new PathsCompiler(""))
    );

    private static final ElasticsearchResource elasticsearch = new ElasticsearchResource();

    private SchemaManager schemaManager;

    @BeforeSuite
    public void before() throws Throwable {
        elasticsearch.before();
        schemaManager = new SchemaManager(elasticsearch.client(), frontendIndexFactory, consumersIndexFactory, false);
    }

    @AfterSuite
    public void after() {
        elasticsearch.after();
    }

    @Override
    protected LogRepository createRepository() {
        schemaManager.ensureSchema();

        return new FrontendElasticsearchLogRepository.Builder(elasticsearch.client(), metricsFacade)
                .withIndexFactory(frontendIndexFactory)
                .build();
    }

    @Override
    protected void awaitUntilPublishMessageIsPersisted(String topic,
                                                       String id,
                                                       String remoteHostname,
                                                       String datacenter,
                                                       String... extraRequestHeadersKeywords) throws Exception {
        awaitUntilMessageIsIndexed(getQuery(topic, id, PublishedMessageTraceStatus.SUCCESS, remoteHostname, extraRequestHeadersKeywords)
                .must(matchQuery(DATACENTER, datacenter))
        );
    }

    @Override
    protected void awaitUntilInflightMessageIsPersisted(String topic,
                                                        String id,
                                                        String remoteHostname,
                                                        String... extraRequestHeadersKeywords) throws Exception {
        awaitUntilMessageIsIndexed(getQuery(topic, id, PublishedMessageTraceStatus.INFLIGHT, remoteHostname, extraRequestHeadersKeywords));
    }

    @Override
    protected void awaitUntilErrorMessageIsPersisted(String topic,
                                                     String id,
                                                     String reason,
                                                     String remoteHostname,
                                                     String... extraRequestHeadersKeywords) throws Exception {
        awaitUntilMessageIsIndexed(
                getQuery(topic, id, PublishedMessageTraceStatus.ERROR, remoteHostname, extraRequestHeadersKeywords)
                        .must(matchQuery(REASON, reason)));
    }

    private BoolQueryBuilder getQuery(String topic,
                                      String id,
                                      PublishedMessageTraceStatus status,
                                      String remoteHostname,
                                      String... extraRequestHeadersKeywords) {
        BoolQueryBuilder queryBuilder = boolQuery()
                .must(termQuery(TOPIC_NAME, topic))
                .must(termQuery(MESSAGE_ID, id))
                .must(termQuery(STATUS, status.toString()))
                .must(termQuery(CLUSTER, CLUSTER_NAME))
                .must(termQuery(REMOTE_HOSTNAME, remoteHostname));
        for (String extraRequestHeadersKeyword : extraRequestHeadersKeywords) {
            queryBuilder.must(termQuery(EXTRA_REQUEST_HEADERS, extraRequestHeadersKeyword));
        }
        return queryBuilder;
    }

    private void awaitUntilMessageIsIndexed(QueryBuilder query) {
        await().atMost(ONE_MINUTE).until(() -> {
            SearchResponse response = elasticsearch.client().prepareSearch(frontendIndexFactory.createIndex())
                    .setTypes(SchemaManager.PUBLISHED_TYPE)
                    .setQuery(query)
                    .execute().get();
            return response.getHits().getTotalHits() == 1;
        });
    }

}