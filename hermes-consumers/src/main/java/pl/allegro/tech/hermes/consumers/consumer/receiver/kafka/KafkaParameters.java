package pl.allegro.tech.hermes.consumers.consumer.receiver.kafka;

public interface KafkaParameters {

    boolean isEnabled();

    String getMechanism();

    String getProtocol();

    String getUsername();

    String getPassword();

    String getBrokerList();
}
