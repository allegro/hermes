package pl.allegro.tech.hermes.frontend.config;

import pl.allegro.tech.hermes.common.config.KafkaAuthorizationProperties;
import pl.allegro.tech.hermes.common.kafka.KafkaParameters;

public class KafkaProperties {

    private KafkaAuthorizationProperties authorization = new KafkaAuthorizationProperties();

    private String datacenter = "dc";

    private String clusterName = "primary";

    private String brokerList = "localhost:9092";

    private int adminRequestTimeoutMs = 5 * 60 * 1000;

    public KafkaAuthorizationProperties getAuthorization() {
        return authorization;
    }

    public void setAuthorization(KafkaAuthorizationProperties authorization) {
        this.authorization = authorization;
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

    public String getBrokerList() {
        return brokerList;
    }

    public void setBrokerList(String brokerList) {
        this.brokerList = brokerList;
    }

    public int getAdminRequestTimeoutMs() {
        return adminRequestTimeoutMs;
    }

    public void setAdminRequestTimeoutMs(int adminRequestTimeoutMs) {
        this.adminRequestTimeoutMs = adminRequestTimeoutMs;
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
