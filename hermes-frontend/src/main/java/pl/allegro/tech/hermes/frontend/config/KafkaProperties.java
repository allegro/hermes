package pl.allegro.tech.hermes.frontend.config;

import pl.allegro.tech.hermes.common.config.KafkaAuthenticationProperties;
import pl.allegro.tech.hermes.common.kafka.KafkaParameters;

import java.time.Duration;

public class KafkaProperties implements KafkaParameters {

    private KafkaAuthenticationProperties authorization = new KafkaAuthenticationProperties();

    private String datacenter = "dc";

    private String brokerList = "localhost:9092";

    private Duration adminRequestTimeout = Duration.ofMinutes(5);

    public KafkaAuthenticationProperties getAuthorization() {
        return authorization;
    }

    public void setAuthorization(KafkaAuthenticationProperties authorization) {
        this.authorization = authorization;
    }

    public String getDatacenter() {
        return datacenter;
    }

    public void setDatacenter(String datacenter) {
        this.datacenter = datacenter;
    }

    @Override
    public boolean isEnabled() {
        return authorization.isEnabled();
    }

    @Override
    public String getMechanism() {
        return authorization.getMechanism();
    }

    @Override
    public String getProtocol() {
        return authorization.getProtocol();
    }

    @Override
    public String getUsername() {
        return authorization.getUsername();
    }

    @Override
    public String getPassword() {
        return authorization.getPassword();
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
        return authorization.getJaasConfig();
    }
}
