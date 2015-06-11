package pl.allegro.tech.hermes.message.tracker.elasticsearch.frontend;

import org.elasticsearch.client.Client;
import org.elasticsearch.common.xcontent.XContentBuilder;
import pl.allegro.tech.hermes.api.PublishedMessageTraceStatus;
import pl.allegro.tech.hermes.message.tracker.elasticsearch.ElasticsearchRepositoryException;
import pl.allegro.tech.hermes.message.tracker.elasticsearch.LogSchemaAware;
import pl.allegro.tech.hermes.message.tracker.frontend.LogRepository;

import java.io.IOException;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;
import static pl.allegro.tech.hermes.api.PublishedMessageTraceStatus.SUCCESS;

public class ElasticsearchLogRepository implements LogRepository, LogSchemaAware {

    private final Client elasticClient;

    public ElasticsearchLogRepository(Client elasticClient) {
        this.elasticClient = elasticClient;
    }

    @Override
    public void logPublished(String messageId, long timestamp, String topicName) {
        try {
            elasticClient.prepareIndex(PUBLISHED_INDEX, PUBLISHED_TYPE)
                    .setSource(document(messageId, timestamp, topicName, SUCCESS))
                    .execute();
        } catch (IOException ex) {
            throw new ElasticsearchRepositoryException(ex);
        }
    }

    @Override
    public void logError(String messageId, long timestamp, String topicName, String reason) {

    }

    @Override
    public void logInflight(String messageId, long timestamp, String topicName) {

    }

    private XContentBuilder document(String messageId, long timestamp, String topicName, PublishedMessageTraceStatus status)
            throws IOException {
        return jsonBuilder()
                .startObject()
                    .field(MESSAGE_ID, messageId)
                    .field(TIMESTAMP, timestamp)
                    .field(TOPIC_NAME, topicName)
                    .field(STATUS, status)
                .endObject();
    }

}
