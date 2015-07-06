package pl.allegro.tech.hermes.tracker.elasticsearch;

public interface LogSchemaAware {

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

    enum TypedIndex implements LogSchemaAware {

        PUBLISHED_MESSAGES("published_messages", "published_message"), SENT_MESSAGES("sent_messages", "sent_message");

        private final String index;
        private final String type;

        TypedIndex(String index, String type) {
            this.index = index;
            this.type = type;
        }

        public String getIndex() {
            return index;
        }

        public String getType() {
            return type;
        }
    }
}
