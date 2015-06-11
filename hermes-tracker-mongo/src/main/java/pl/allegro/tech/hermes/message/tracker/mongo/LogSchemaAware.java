package pl.allegro.tech.hermes.message.tracker.mongo;

public interface LogSchemaAware {

    String COLLECTION_PUBLISHED_NAME = "published_messages";
    String COLLECTION_SENT_NAME = "sent_messages";
    String MESSAGE_ID = "messageId";
    String CREATED_AT = "createdAt";
    String TIMESTAMP = "timestamp";
    String PUBLISH_TIMESTAMP = "publish_timestamp";
    String STATUS = "status";
    String TOPIC_NAME = "topicName";
    String SUBSCRIPTION = "subscription";
    String PARTITION = "partition";
    String OFFSET = "offset";
    String REASON = "reason";
    String CLUSTER = "cluster";

}
