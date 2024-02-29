package pl.allegro.tech.hermes.common.kafka;

public interface KafkaParameters {

    boolean isAuthenticationEnabled();

    String getAuthenticationMechanism();

    String getAuthenticationProtocol();

    String getBrokerList();

    String getJaasConfig();

    String getDatacenter();
}
