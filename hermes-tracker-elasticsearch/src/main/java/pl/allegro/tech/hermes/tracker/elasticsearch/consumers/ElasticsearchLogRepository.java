package pl.allegro.tech.hermes.tracker.elasticsearch.consumers;

import org.elasticsearch.client.Client;
import org.elasticsearch.common.xcontent.XContentBuilder;
import pl.allegro.tech.hermes.api.SentMessageTraceStatus;
import pl.allegro.tech.hermes.tracker.consumers.LogRepository;
import pl.allegro.tech.hermes.tracker.consumers.MessageMetadata;
import pl.allegro.tech.hermes.tracker.elasticsearch.AbstractLogRepository;
import pl.allegro.tech.hermes.tracker.elasticsearch.LogSchemaAware;

import java.io.IOException;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;
import static pl.allegro.tech.hermes.api.SentMessageTraceStatus.*;

public class ElasticsearchLogRepository extends AbstractLogRepository implements LogRepository, LogSchemaAware {

    public ElasticsearchLogRepository(Client elasticClient, String clusterName) {
        super(elasticClient, clusterName, SENT_INDEX, SENT_TYPE);
    }

    @Override
    public void logSuccessful(MessageMetadata message, long timestamp) {
        indexDocument(() -> document(message, timestamp, SUCCESS));
    }

    @Override
    public void logFailed(MessageMetadata message, long timestamp, String reason) {
        indexDocument(() -> document(message, timestamp, FAILED, reason));
    }

    @Override
    public void logDiscarded(MessageMetadata message, long timestamp, String reason) {
        indexDocument(() -> document(message, timestamp, DISCARDED, reason));
    }

    @Override
    public void logInflight(MessageMetadata message, long timestamp) {
        indexDocument(() -> document(message, timestamp, INFLIGHT));
    }

    private XContentBuilder document(MessageMetadata message, long createdAt, SentMessageTraceStatus status) throws IOException {
        return notEndedDocument(message, createdAt, status.toString()).endObject();
    }

    private XContentBuilder document(MessageMetadata message, long timestamp, SentMessageTraceStatus status, String reason)
            throws IOException {
        return notEndedDocument(message, timestamp, status.toString()).field(REASON, reason).endObject();
    }

    protected XContentBuilder notEndedDocument(MessageMetadata message, long timestamp, String status)
            throws IOException {
        return jsonBuilder()
                .startObject()
                .field(MESSAGE_ID, message.getId())
                .field(TIMESTAMP, timestamp)
                .field(PUBLISH_TIMESTAMP, message.getPublishingTimestamp().orElse(null))
                .field(TOPIC_NAME, message.getTopic())
                .field(SUBSCRIPTION, message.getSubscription())
                .field(STATUS, status)
                .field(OFFSET, message.getOffset())
                .field(PARTITION, message.getPartition())
                .field(CLUSTER, clusterName);
    }

}
