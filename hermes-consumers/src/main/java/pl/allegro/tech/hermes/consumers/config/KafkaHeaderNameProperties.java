package pl.allegro.tech.hermes.consumers.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import pl.allegro.tech.hermes.common.kafka.KafkaHeaderNameParameters;

import java.util.Set;

@ConfigurationProperties(prefix = "consumer.kafka.header.name")
public class KafkaHeaderNameProperties implements KafkaHeaderNameParameters {

    private String schemaVersion = "sv";

    private String schemaId = "sid";

    private String messageId = "id";

    private String timestamp = "ts";

    private Set<String> internalHeaders = Set.of(messageId, timestamp, schemaId, schemaVersion);

    @Override
    public String getSchemaVersion() {
        return schemaVersion;
    }

    public void setSchemaVersion(String schemaVersion) {
        updateInternalHeaders(this.schemaVersion, schemaVersion);
        this.schemaVersion = schemaVersion;
    }

    @Override
    public String getSchemaId() {
        return schemaId;
    }

    public void setSchemaId(String schemaId) {
        updateInternalHeaders(this.schemaId, schemaId);
        this.schemaId = schemaId;
    }

    @Override
    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        updateInternalHeaders(this.messageId, messageId);
        this.messageId = messageId;
    }

    @Override
    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        updateInternalHeaders(this.timestamp, timestamp);
        this.timestamp = timestamp;
    }

    public boolean isNotInternal(String name) {
        return !internalHeaders.contains(name);
    }

    private void updateInternalHeaders(String oldHeader, String newHeader) {
        internalHeaders.remove(oldHeader);
        internalHeaders.add(newHeader);
    }
}
