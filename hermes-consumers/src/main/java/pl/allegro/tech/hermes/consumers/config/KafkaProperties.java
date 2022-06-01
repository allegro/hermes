package pl.allegro.tech.hermes.consumers.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import pl.allegro.tech.hermes.consumers.consumer.receiver.kafka.KafkaParameters;

@ConfigurationProperties(prefix = "kafka")
public class KafkaProperties {

    private KafkaAuthorizationProperties authorization;
    private String clusterName = "primary-dc";
    private String brokerList = "kafka.broker.list, localhost:9092";

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

    public String getBrokerList() {
        return brokerList;
    }

    public void setBrokerList(String brokerList) {
        this.brokerList = brokerList;
    }

    protected KafkaParameters toKafkaAuthorizationParameters() {
        return new KafkaParameters(
                this.authorization.isEnabled(),
                this.authorization.getMechanism(),
                this.authorization.getProtocol(),
                this.authorization.getUsername(),
                this.authorization.getPassword(),
                this.brokerList
        );
    }
}
