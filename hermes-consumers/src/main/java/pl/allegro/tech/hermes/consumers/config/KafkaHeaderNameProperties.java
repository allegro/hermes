package pl.allegro.tech.hermes.consumers.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import pl.allegro.tech.hermes.common.kafka.KafkaHeaderNameParameters;

import java.util.Set;

@ConfigurationProperties(prefix = "consumer.kafka.header.name")
public class KafkaHeaderNameProperties implements KafkaHeaderNameParameters {

    private String schemaVersion = "sv";

    private String schemaId = "sid";

    private String messageId = "id";

    // compatibility header, can be removed when all messages on Kafka don't have the header
    private String timestamp = "ts";

    private Set<String> internalHeaders = Set.of(messageId, timestamp, schemaId, schemaVersion);

    @Override
    public String getSchemaVersion() {
        return schemaVersion;
    }

    public void setSchemaVersion(String schemaVersion) {
        this.schemaVersion = schemaVersion;
        updateInternalHeaders();
    }

    @Override
    public String getSchemaId() {
        return schemaId;
    }

    public void setSchemaId(String schemaId) {
        this.schemaId = schemaId;
        updateInternalHeaders();
    }

    @Override
    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
        updateInternalHeaders();
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
        updateInternalHeaders();
    }

    public boolean isNotInternal(String name) {
        return !internalHeaders.contains(name);
    }

    private void updateInternalHeaders() {
        internalHeaders = Set.of(messageId, schemaId, schemaVersion, timestamp);
    }
}
