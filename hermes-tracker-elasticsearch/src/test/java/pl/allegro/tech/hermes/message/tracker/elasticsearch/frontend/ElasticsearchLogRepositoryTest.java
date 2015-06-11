package pl.allegro.tech.hermes.message.tracker.elasticsearch.frontend;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import pl.allegro.tech.hermes.api.PublishedMessageTraceStatus;
import pl.allegro.tech.hermes.message.tracker.elasticsearch.LogSchemaAware;

import java.io.File;
import java.nio.file.Files;

import static com.jayway.awaitility.Awaitility.await;
import static com.jayway.awaitility.Duration.FIVE_SECONDS;
import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.matchQuery;
import static pl.allegro.tech.hermes.api.PublishedMessageTraceStatus.SUCCESS;

public class ElasticsearchLogRepositoryTest implements LogSchemaAware {

    private ElasticsearchLogRepository logRepository;
    private Node elastic;
    private Client client;

    @Before
    public void setUp() throws Exception {
        File dataDir = Files.createTempDirectory("elasticsearch_data_").toFile();
        Settings settings = ImmutableSettings.settingsBuilder()
                .put("path.data", dataDir)
                .put("cluster.name", "hermes").build();
        elastic = NodeBuilder.nodeBuilder().local(true).settings(settings).build();
        elastic.start();
        client = elastic.client();
        client.admin().indices().prepareCreate(PUBLISHED_INDEX).execute().actionGet();
        client.admin().cluster().prepareHealth(PUBLISHED_INDEX).setWaitForActiveShards(1).execute().actionGet();
        logRepository = new ElasticsearchLogRepository(client);
    }

    @After
    public void tearDown() throws Exception {
        elastic.stop();
    }

    @Test
    public void shouldLogPublishedMessage() throws Exception {
        // given
        String id = "sentMessage";
        String topic = "group.sentMessage";

        // when
        logRepository.logPublished(id, 1234L, topic);

        // then
        awaitUntilMessageIsIndexed(topic, id, SUCCESS);
    }

    private void awaitUntilMessageIsIndexed(String topic, String id, PublishedMessageTraceStatus status) throws Exception {
        await().atMost(FIVE_SECONDS).until(() -> {
            SearchResponse response = client.prepareSearch(PUBLISHED_INDEX)
                    .setTypes(PUBLISHED_TYPE)
                    .setQuery(boolQuery()
                                .should(matchQuery(TOPIC_NAME, topic))
                                .should(matchQuery(MESSAGE_ID, id))
                                .should(matchQuery(STATUS, status.toString())))
                    .execute().get();
            return response.getHits().getTotalHits() == 1;
        });
    }

}