package pl.allegro.tech.hermes.consumers.config;

import pl.allegro.tech.hermes.common.config.KafkaAuthenticationProperties;
import pl.allegro.tech.hermes.common.kafka.KafkaParameters;

public class KafkaProperties implements KafkaParameters {

  private KafkaAuthenticationProperties authentication = new KafkaAuthenticationProperties();

  private String datacenter = "dc";

  private String clusterName = "primary";

  private String brokerList = "localhost:9092";

  public KafkaAuthenticationProperties getAuthentication() {
    return authentication;
  }

  @Deprecated
  public void setAuthorization(KafkaAuthenticationProperties authorization) {
    this.authentication = authorization;
  }

  public void setAuthentication(KafkaAuthenticationProperties authentication) {
    this.authentication = authentication;
  }

  public String getDatacenter() {
    return datacenter;
  }

  public void setDatacenter(String datacenter) {
    this.datacenter = datacenter;
  }

  public String getClusterName() {
    return clusterName;
  }

  public void setClusterName(String clusterName) {
    this.clusterName = clusterName;
  }

  @Override
  public boolean isAuthenticationEnabled() {
    return authentication.isEnabled();
  }

  @Override
  public String getAuthenticationMechanism() {
    return authentication.getMechanism();
  }

  @Override
  public String getAuthenticationProtocol() {
    return authentication.getProtocol();
  }

  @Override
  public String getBrokerList() {
    return brokerList;
  }

  public void setBrokerList(String brokerList) {
    this.brokerList = brokerList;
  }

  @Override
  public String getJaasConfig() {
    return authentication.getJaasConfig();
  }
}
