package pl.allegro.tech.hermes.common.kafka;

public interface KafkaParameters {

  String getDatacenter();

  boolean isAuthenticationEnabled();

  String getAuthenticationMechanism();

  String getAuthenticationProtocol();

  String getBrokerList();

  String getJaasConfig();
}
