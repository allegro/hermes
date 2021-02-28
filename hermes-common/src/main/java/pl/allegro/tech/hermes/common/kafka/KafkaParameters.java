package pl.allegro.tech.hermes.common.kafka;

import pl.allegro.tech.hermes.common.config.KafkaSSLProperties;

public interface KafkaParameters {

    boolean isEnabled();

    String getMechanism();

    String getProtocol();

    String getUsername();

    String getPassword();

    String getBrokerList();

    KafkaSSLProperties getSsl();
}
