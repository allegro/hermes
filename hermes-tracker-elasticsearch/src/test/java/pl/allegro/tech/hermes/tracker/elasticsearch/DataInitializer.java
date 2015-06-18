package pl.allegro.tech.hermes.tracker.elasticsearch;

import org.elasticsearch.client.Client;
import org.elasticsearch.common.xcontent.XContentBuilder;
import pl.allegro.tech.hermes.api.PublishedMessageTraceStatus;
import pl.allegro.tech.hermes.api.SentMessageTraceStatus;
import pl.allegro.tech.hermes.tracker.consumers.MessageMetadata;

import java.io.IOException;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

public class DataInitializer implements LogSchemaAware {

    private final Client client;
    private final String clusterName;

    public DataInitializer(Client client, String clusterName) {
        this.client = client;
        this.clusterName = clusterName;
    }

    public void indexPublishedMessage(MessageMetadata messageMetadata, long timestamp, PublishedMessageTraceStatus status) throws IOException {
        XContentBuilder publishedContent = jsonBuilder()
                .startObject()
                .field(MESSAGE_ID, messageMetadata.getId())
                .field(TIMESTAMP, timestamp)
                .field(STATUS, status)
                .field(TOPIC_NAME, messageMetadata.getTopic())
                .field(CLUSTER, clusterName)
                .endObject();

        client.prepareIndex(PUBLISHED_INDEX, PUBLISHED_TYPE)
                .setSource(publishedContent)
                .execute();
    }

    public void indexSentMessage(MessageMetadata messageMetadata, long timestamp, SentMessageTraceStatus status, String reason) throws IOException {
        XContentBuilder content = jsonBuilder()
                .startObject()
                .field(MESSAGE_ID, messageMetadata.getId())
                .field(TIMESTAMP, timestamp)
                .field(PUBLISH_TIMESTAMP, messageMetadata.getPublishingTimestamp().get())
                .field(TOPIC_NAME, messageMetadata.getTopic())
                .field(SUBSCRIPTION, messageMetadata.getSubscription())
                .field(STATUS, status)
                .field(OFFSET, messageMetadata.getOffset())
                .field(PARTITION, messageMetadata.getPartition())
                .field(CLUSTER, clusterName)
                .field(REASON, reason)
                .endObject();

        client.prepareIndex(SENT_INDEX, SENT_TYPE)
                .setSource(content)
                .execute();
    }
}
