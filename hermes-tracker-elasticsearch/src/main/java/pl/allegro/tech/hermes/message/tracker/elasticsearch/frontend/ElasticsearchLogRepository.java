package pl.allegro.tech.hermes.message.tracker.elasticsearch.frontend;

import org.elasticsearch.client.Client;
import org.elasticsearch.common.xcontent.XContentBuilder;
import pl.allegro.tech.hermes.api.PublishedMessageTraceStatus;
import pl.allegro.tech.hermes.message.tracker.elasticsearch.AbstractLogRepository;
import pl.allegro.tech.hermes.message.tracker.elasticsearch.LogSchemaAware;
import pl.allegro.tech.hermes.message.tracker.frontend.LogRepository;

import java.io.IOException;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;
import static pl.allegro.tech.hermes.api.PublishedMessageTraceStatus.*;

public class ElasticsearchLogRepository extends AbstractLogRepository implements LogRepository, LogSchemaAware {

    public ElasticsearchLogRepository(Client elasticClient, String clusterName) {
        super(elasticClient, clusterName, PUBLISHED_INDEX, PUBLISHED_TYPE);
    }

    @Override
    public void logPublished(String messageId, long timestamp, String topicName) {
        indexDocument(() -> document(messageId, timestamp, topicName, SUCCESS));
    }

    @Override
    public void logError(String messageId, long timestamp, String topicName, String reason) {
        indexDocument(() -> document(messageId, timestamp, topicName, ERROR, reason));
    }

    @Override
    public void logInflight(String messageId, long timestamp, String topicName) {
        indexDocument(() -> document(messageId, timestamp, topicName, INFLIGHT));
    }

    private XContentBuilder document(String messageId, long timestamp, String topicName, PublishedMessageTraceStatus status)
            throws IOException {
        return notEndedDocument(messageId, timestamp, topicName, status.toString()).endObject();
    }

    private XContentBuilder document(String messageId, long timestamp, String topicName, PublishedMessageTraceStatus status, String reason)
            throws IOException {
        return notEndedDocument(messageId, timestamp, topicName, status.toString()).field(REASON, reason).endObject();
    }

    protected XContentBuilder notEndedDocument(String messageId, long timestamp, String topicName, String status)
            throws IOException {
        return jsonBuilder()
                .startObject()
                .field(MESSAGE_ID, messageId)
                .field(TIMESTAMP, timestamp)
                .field(TOPIC_NAME, topicName)
                .field(STATUS, status)
                .field(CLUSTER, clusterName);
    }
}
