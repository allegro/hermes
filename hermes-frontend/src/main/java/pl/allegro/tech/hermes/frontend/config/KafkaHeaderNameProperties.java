package pl.allegro.tech.hermes.frontend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import pl.allegro.tech.hermes.frontend.producer.kafka.KafkaHeaderNameParameters;

@ConfigurationProperties(prefix = "frontend.kafka.header.name")
public class KafkaHeaderNameProperties {

    private String messageId = "id";

    private String timestamp = "ts";

    private String schemaVersion = "sv";

    private String schemaId = "sid";

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getSchemaVersion() {
        return schemaVersion;
    }

    public void setSchemaVersion(String schemaVersion) {
        this.schemaVersion = schemaVersion;
    }

    public String getSchemaId() {
        return schemaId;
    }

    public void setSchemaId(String schemaId) {
        this.schemaId = schemaId;
    }

    public KafkaHeaderNameParameters toKafkaHeaderNameParameters() {
        return new KafkaHeaderNameParameters(
                this.messageId,
                this.timestamp,
                this.schemaVersion,
                this.schemaId
        );
    }
}
