package pl.allegro.tech.hermes.tracker.elasticsearch;

public interface LogSchemaAware {

    String MESSAGE_ID = "messageId";
    String BATCH_ID = "batchId";
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
