package pl.allegro.tech.hermes.message.tracker.elasticsearch.consumers;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import pl.allegro.tech.hermes.api.SentMessageTraceStatus;
import pl.allegro.tech.hermes.message.tracker.elasticsearch.LogSchemaAware;
import pl.allegro.tech.hermes.message.tracker.consumers.AbstractLogRepositoryTest;
import pl.allegro.tech.hermes.message.tracker.consumers.LogRepository;

import java.io.File;
import java.nio.file.Files;

import static com.jayway.awaitility.Awaitility.await;
import static com.jayway.awaitility.Duration.FIVE_SECONDS;
import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.matchQuery;

public class ElasticsearchLogRepositoryTest extends AbstractLogRepositoryTest implements LogSchemaAware {

    public static final String CLUSTER_NAME = "primary";
    private static Node elastic;
    private static Client client;

    @BeforeClass
    public static void setupSpec() throws Exception {
        File dataDir = Files.createTempDirectory("elasticsearch_data_").toFile();
        Settings settings = ImmutableSettings.settingsBuilder()
                .put("path.data", dataDir)
                .put("cluster.name", "hermes").build();
        elastic = NodeBuilder.nodeBuilder().local(true).settings(settings).build();
        elastic.start();
        client = elastic.client();
        client.admin().indices().prepareCreate(SENT_INDEX).execute().actionGet();
        client.admin().cluster().prepareHealth(SENT_INDEX).setWaitForActiveShards(1).execute().actionGet();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        elastic.stop();
    }

    @Override
    protected LogRepository createLogRepository() {
        return new ElasticsearchLogRepository(client, CLUSTER_NAME);
    }

    @Override
    protected void awaitUntilMessageIsPersisted(String topic, String subscription, String id, SentMessageTraceStatus status) throws Exception {
        await().atMost(FIVE_SECONDS).until(() -> {
            SearchResponse response = client.prepareSearch(SENT_INDEX)
                    .setTypes(SENT_TYPE)
                    .setQuery(boolQuery()
                        .should(matchQuery(TOPIC_NAME, topic))
                        .should(matchQuery(SUBSCRIPTION, subscription))
                        .should(matchQuery(MESSAGE_ID, id))
                        .should(matchQuery(STATUS, status.toString()))
                        .should(matchQuery(CLUSTER, CLUSTER_NAME)))
                    .execute().get();
            return response.getHits().getTotalHits() == 1;
        });
    }
}
