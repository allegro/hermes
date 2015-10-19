package pl.allegro.tech.hermes.tracker.elasticsearch.consumers;

import com.codahale.metrics.MetricRegistry;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.FilterBuilders;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import pl.allegro.tech.hermes.api.SentMessageTraceStatus;
import pl.allegro.tech.hermes.metrics.PathsCompiler;
import pl.allegro.tech.hermes.tracker.consumers.AbstractLogRepositoryTest;
import pl.allegro.tech.hermes.tracker.consumers.LogRepository;
import pl.allegro.tech.hermes.tracker.elasticsearch.DataInitializer;
import pl.allegro.tech.hermes.tracker.elasticsearch.ElasticsearchResource;
import pl.allegro.tech.hermes.tracker.elasticsearch.LogSchemaAware;
import pl.allegro.tech.hermes.tracker.elasticsearch.SchemaManager;
import pl.allegro.tech.hermes.tracker.elasticsearch.frontend.FrontendDailyIndexFactory;
import pl.allegro.tech.hermes.tracker.elasticsearch.frontend.FrontendIndexFactory;
import pl.allegro.tech.hermes.tracker.elasticsearch.management.ElasticsearchLogRepository;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static com.jayway.awaitility.Awaitility.await;
import static com.jayway.awaitility.Duration.ONE_MINUTE;
import static org.elasticsearch.index.query.QueryBuilders.filteredQuery;
import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;

public class ConsumersElasticsearchLogRepositoryTest extends AbstractLogRepositoryTest implements LogSchemaAware {

    private static final String CLUSTER_NAME = "primary";

    private static final Clock clock = Clock.fixed(LocalDate.of(2000, 1, 1).atStartOfDay().toInstant(ZoneOffset.UTC), ZoneId.systemDefault());
    private static final ConsumersIndexFactory indexFactory = new ConsumersDailyIndexFactory(clock);
    private static final FrontendIndexFactory frontendIndexFactory = new FrontendDailyIndexFactory(clock);

    public static ElasticsearchResource elasticsearch = new ElasticsearchResource(indexFactory);
    private SchemaManager schemaManager;

    @BeforeClass
    public void before() throws Throwable {
        elasticsearch.before();
        schemaManager = new SchemaManager(elasticsearch.client(), frontendIndexFactory, indexFactory);
    }

    @AfterClass
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
    protected void awaitUntilMessageIsPersisted(String topic, String subscription, String id, SentMessageTraceStatus status) throws Exception {
        await().atMost(ONE_MINUTE).until(() -> {
            SearchResponse response = elasticsearch.client().prepareSearch(indexFactory.createIndex())
                    .setTypes(SchemaManager.SENT_TYPE)
                    .setQuery(filteredQuery(matchAllQuery(),
                            FilterBuilders.andFilter(
                                    FilterBuilders.termFilter(TOPIC_NAME, topic),
                                    FilterBuilders.termFilter(SUBSCRIPTION, subscription),
                                    FilterBuilders.termFilter(MESSAGE_ID, id),
                                    FilterBuilders.termFilter(STATUS, status.toString()),
                                    FilterBuilders.termFilter(CLUSTER, CLUSTER_NAME)
                            )))
                    .execute().get();
            return response.getHits().getTotalHits() == 1;
        });
    }
}
