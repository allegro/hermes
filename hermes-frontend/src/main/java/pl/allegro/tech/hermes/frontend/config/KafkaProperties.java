package pl.allegro.tech.hermes.frontend.config;

import java.time.Duration;
import pl.allegro.tech.hermes.common.config.KafkaAuthenticationProperties;
import pl.allegro.tech.hermes.common.kafka.KafkaParameters;

public class KafkaProperties implements KafkaParameters {

  private KafkaAuthenticationProperties authentication = new KafkaAuthenticationProperties();

  private String datacenter = "dc";

  private String brokerList = "localhost:9092";

  private Duration adminRequestTimeout = Duration.ofMinutes(5);

  @Deprecated
  public void setAuthorization(KafkaAuthenticationProperties authorization) {
    this.authentication = authorization;
  }

  public void setAuthentication(KafkaAuthenticationProperties authorization) {
    this.authentication = authorization;
  }

  public String getDatacenter() {
    return datacenter;
  }

  public void setDatacenter(String datacenter) {
    this.datacenter = datacenter;
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

  public String getBrokerList() {
    return brokerList;
  }

  public void setBrokerList(String brokerList) {
    this.brokerList = brokerList;
  }

  public Duration getAdminRequestTimeout() {
    return adminRequestTimeout;
  }

  public void setAdminRequestTimeout(Duration adminRequestTimeout) {
    this.adminRequestTimeout = adminRequestTimeout;
  }

  @Override
  public String getJaasConfig() {
    return authentication.getJaasConfig();
  }
}
