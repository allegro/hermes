package pl.allegro.tech.hermes.consumers.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import pl.allegro.tech.hermes.common.config.KafkaAuthorizationProperties;
import pl.allegro.tech.hermes.common.kafka.KafkaParameters;

@ConfigurationProperties(prefix = "consumer.kafka")
public class KafkaProperties {

    private KafkaAuthorizationProperties authorization = new KafkaAuthorizationProperties();

    private String clusterName = "primary";

    private String brokerList = "localhost:9092";

    private String namespace = "";

    private String namespaceSeparator = "_";

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

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getNamespaceSeparator() {
        return namespaceSeparator;
    }

    public void setNamespaceSeparator(String namespaceSeparator) {
        this.namespaceSeparator = namespaceSeparator;
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
