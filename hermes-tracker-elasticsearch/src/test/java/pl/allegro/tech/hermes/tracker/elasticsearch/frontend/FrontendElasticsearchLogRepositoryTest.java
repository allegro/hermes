package pl.allegro.tech.hermes.tracker.elasticsearch.frontend;

import com.codahale.metrics.MetricRegistry;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.QueryBuilder;
import org.junit.ClassRule;
import pl.allegro.tech.hermes.api.PublishedMessageTraceStatus;
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
import static org.elasticsearch.index.query.QueryBuilders.filteredQuery;
import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;

public class FrontendElasticsearchLogRepositoryTest extends AbstractLogRepositoryTest implements LogSchemaAware {

    private static final String CLUSTER_NAME = "primary";

    private static final Clock clock = Clock.fixed(LocalDate.of(2000, 1, 1).atStartOfDay().toInstant(ZoneOffset.UTC), ZoneId.systemDefault());
    private static final FrontendIndexFactory frontendIndexFactory = new FrontendDailyIndexFactory(clock);
    private static final ConsumersIndexFactory consumersIndexFactory = new ConsumersDailyIndexFactory(clock);

    @ClassRule
    public static ElasticsearchResource elasticsearch = new ElasticsearchResource(frontendIndexFactory);

    private final SchemaManager schemaManager = new SchemaManager(elasticsearch.client(), frontendIndexFactory, consumersIndexFactory);

    @Override
    protected LogRepository createRepository() {
        schemaManager.ensureSchema();

        return new FrontendElasticsearchLogRepository.Builder(elasticsearch.client(), new PathsCompiler("localhost"), new MetricRegistry())
                .withIndexFactory(frontendIndexFactory)
                .build();
    }

    @Override
    protected void awaitUntilMessageIsPersisted(String topic, String id, PublishedMessageTraceStatus status, String reason) throws Exception {
        awaitUntilMessageIsIndexed(
                filteredQuery(matchAllQuery(),
                        FilterBuilders.andFilter(
                                FilterBuilders.termFilter(TOPIC_NAME, topic),
                                FilterBuilders.termFilter(MESSAGE_ID, id),
                                FilterBuilders.termFilter(STATUS, status.toString()),
                                FilterBuilders.termFilter(REASON, reason),
                                FilterBuilders.termFilter(CLUSTER, CLUSTER_NAME)
                        )));
    }

    @Override
    protected void awaitUntilMessageIsPersisted(String topic, String id, PublishedMessageTraceStatus status) throws Exception {
        awaitUntilMessageIsIndexed(
                filteredQuery(matchAllQuery(),
                        FilterBuilders.andFilter(
                                FilterBuilders.termFilter(TOPIC_NAME, topic),
                                FilterBuilders.termFilter(MESSAGE_ID, id),
                                FilterBuilders.termFilter(STATUS, status.toString()),
                                FilterBuilders.termFilter(CLUSTER, CLUSTER_NAME)
                        )));
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