package pl.allegro.tech.hermes.tracker.elasticsearch.frontend;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.QueryBuilder;
import org.junit.ClassRule;
import pl.allegro.tech.hermes.api.PublishedMessageTraceStatus;
import pl.allegro.tech.hermes.tracker.elasticsearch.ElasticsearchResource;
import pl.allegro.tech.hermes.tracker.elasticsearch.LogSchemaAware;
import pl.allegro.tech.hermes.tracker.frontend.AbstractLogRepositoryTest;
import pl.allegro.tech.hermes.tracker.frontend.LogRepository;

import static com.jayway.awaitility.Awaitility.await;
import static com.jayway.awaitility.Duration.ONE_MINUTE;
import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.matchQuery;

public class ElasticsearchLogRepositoryTest extends AbstractLogRepositoryTest implements LogSchemaAware {

    private static final String CLUSTER_NAME = "primary";

    @ClassRule
    public static ElasticsearchResource elasticsearch = new ElasticsearchResource(PUBLISHED_INDEX);

    @Override
    protected LogRepository createRepository() {
        return new ElasticsearchLogRepository(elasticsearch.client(), CLUSTER_NAME);
    }

    @Override
    protected void awaitUntilMessageIsPersisted(String topic, String id, PublishedMessageTraceStatus status, String reason) throws Exception {
        awaitUntilMessageIsIndexed(
                boolQuery()
                        .should(matchQuery(TOPIC_NAME, topic))
                        .should(matchQuery(MESSAGE_ID, id))
                        .should(matchQuery(STATUS, status.toString()))
                        .should(matchQuery(REASON, reason))
                        .should(matchQuery(CLUSTER, CLUSTER_NAME)));
    }

    @Override
    protected void awaitUntilMessageIsPersisted(String topic, String id, PublishedMessageTraceStatus status) throws Exception {
        awaitUntilMessageIsIndexed(
                boolQuery()
                        .should(matchQuery(TOPIC_NAME, topic))
                        .should(matchQuery(MESSAGE_ID, id))
                        .should(matchQuery(STATUS, status.toString()))
                        .should(matchQuery(CLUSTER, CLUSTER_NAME)));
    }

    private void awaitUntilMessageIsIndexed(QueryBuilder query) {
        await().atMost(ONE_MINUTE).until(() -> {
            SearchResponse response = elasticsearch.client().prepareSearch(PUBLISHED_INDEX)
                    .setTypes(PUBLISHED_TYPE)
                    .setQuery(query)
                    .execute().get();
            return response.getHits().getTotalHits() == 1;
        });
    }

}