package pl.allegro.tech.hermes.tracker.elasticsearch;

public interface LogSchemaAware {

    String PUBLISHED_INDEX = "published_messages";
    String PUBLISHED_TYPE = "published_message";
    String SENT_INDEX = "sent_messages";
    String SENT_TYPE = "sent_message";
    String MESSAGE_ID = "messageId";
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
