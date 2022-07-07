package pl.allegro.tech.hermes.frontend.producer.kafka;

public class KafkaHeaderNameParameters {

    private final String messageId;

    private final String timestamp;

    private final String schemaVersion;

    private final String schemaId;

    public String getMessageId() {
        return messageId;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getSchemaVersion() {
        return schemaVersion;
    }

    public String getSchemaId() {
        return schemaId;
    }

    public KafkaHeaderNameParameters(String messageId, String timestamp, String schemaVersion, String schemaId) {
        this.messageId = messageId;
        this.timestamp = timestamp;
        this.schemaVersion = schemaVersion;
        this.schemaId = schemaId;
    }
}
