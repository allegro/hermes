package pl.allegro.tech.hermes.consumers.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import pl.allegro.tech.hermes.consumers.consumer.receiver.kafka.KafkaParameters;

@ConfigurationProperties(prefix = "kafka")
public class KafkaProperties implements KafkaParameters {

    private KafkaAuthorizationProperties authorization;
    private String clusterName = "primary-dc";
    private String brokerList = "localhost:9092";

    public KafkaAuthorizationProperties getAuthorization() {
        return authorization;
    }

    public void setAuthorization(KafkaAuthorizationProperties authorization) {
        this.authorization = authorization;
    }

    public String getClusterName() {
        return clusterName;
    }

    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
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

    @Override
    public String getBrokerList() {
        return brokerList;
    }

    public void setBrokerList(String brokerList) {
        this.brokerList = brokerList;
    }
}
