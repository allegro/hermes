package pl.allegro.tech.hermes.common.kafka;

public interface KafkaParameters {

    boolean isEnabled();

    String getMechanism();

    String getProtocol();

    String getUsername();

    String getPassword();

    String getBrokerList();

    String getJaasConfig();
}
