package pl.allegro.tech.hermes.frontend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import pl.allegro.tech.hermes.common.kafka.KafkaHeaderNameParameters;

@ConfigurationProperties(prefix = "frontend.kafka.header.name")
public class KafkaHeaderNameProperties implements KafkaHeaderNameParameters {

    private String messageId = "id";

    private String schemaVersion = "sv";

    private String schemaId = "sid";

    @Override
    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    @Override
    public String getSchemaVersion() {
        return schemaVersion;
    }

    public void setSchemaVersion(String schemaVersion) {
        this.schemaVersion = schemaVersion;
    }

    @Override
    public String getSchemaId() {
        return schemaId;
    }

    public void setSchemaId(String schemaId) {
        this.schemaId = schemaId;
    }
}
