package pl.allegro.tech.hermes.consumers.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import pl.allegro.tech.hermes.common.kafka.KafkaHeaderNameParameters;

@ConfigurationProperties(prefix = "consumer.kafka.header.name")
public class KafkaHeaderNameProperties implements KafkaHeaderNameParameters {

  private String schemaVersion = "sv";

  private String schemaId = "sid";

  private String messageId = "id";

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

  @Override
  public String getMessageId() {
    return messageId;
  }

  public void setMessageId(String messageId) {
    this.messageId = messageId;
  }
}
