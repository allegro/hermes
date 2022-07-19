package pl.allegro.tech.hermes.frontend.producer.kafka;

public interface KafkaHeaderNameParameters {

    String getMessageId();

    String getTimestamp();

    String getSchemaVersion();

    String getSchemaId();
}
