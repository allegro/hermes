package pl.allegro.tech.hermes.tracker.elasticsearch;

import org.elasticsearch.client.Client;
import org.elasticsearch.common.xcontent.XContentBuilder;
import pl.allegro.tech.hermes.api.PublishedMessageTraceStatus;
import pl.allegro.tech.hermes.api.SentMessageTraceStatus;
import pl.allegro.tech.hermes.tracker.consumers.MessageMetadata;
import pl.allegro.tech.hermes.tracker.elasticsearch.consumers.ConsumersIndexFactory;
import pl.allegro.tech.hermes.tracker.elasticsearch.frontend.FrontendIndexFactory;

import java.io.IOException;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

public class DataInitializer implements LogSchemaAware {

    private final Client client;
    private final FrontendIndexFactory publishedIndexFactory;
    private final ConsumersIndexFactory sentIndexFactory;
    private final String clusterName;

    public DataInitializer(Client client,
                           FrontendIndexFactory publishedIndexFactory,
                           ConsumersIndexFactory sentIndexFactory,
                           String clusterName) {
        this.client = client;
        this.publishedIndexFactory = publishedIndexFactory;
        this.sentIndexFactory = sentIndexFactory;
        this.clusterName = clusterName;
    }

    public void indexPublishedMessage(MessageMetadata messageMetadata, long timestamp, PublishedMessageTraceStatus status) throws IOException {
        XContentBuilder publishedContent = jsonBuilder()
                .startObject()
                .field(MESSAGE_ID, messageMetadata.getMessageId())
                .field(TIMESTAMP, timestamp)
                .field(STATUS, status)
                .field(TOPIC_NAME, messageMetadata.getTopic())
                .field(CLUSTER, clusterName)
                .endObject();

        client.prepareIndex(publishedIndexFactory.createIndex(), SchemaManager.PUBLISHED_TYPE)
                .setSource(publishedContent)
                .execute();
    }

    public void indexSentMessage(MessageMetadata messageMetadata, long timestamp, SentMessageTraceStatus status, String reason) throws IOException {
        XContentBuilder content = jsonBuilder()
                .startObject()
                .field(MESSAGE_ID, messageMetadata.getMessageId())
                .field(BATCH_ID, messageMetadata.getBatchId())
                .field(TIMESTAMP, timestamp)
                .field(PUBLISH_TIMESTAMP, messageMetadata.getPublishingTimestamp())
                .field(TOPIC_NAME, messageMetadata.getTopic())
                .field(SUBSCRIPTION, messageMetadata.getSubscription())
                .field(STATUS, status)
                .field(OFFSET, messageMetadata.getOffset())
                .field(PARTITION, messageMetadata.getPartition())
                .field(CLUSTER, clusterName)
                .field(REASON, reason)
                .endObject();

        client.prepareIndex(sentIndexFactory.createIndex(), SchemaManager.SENT_TYPE)
                .setSource(content)
                .execute();
    }
}
