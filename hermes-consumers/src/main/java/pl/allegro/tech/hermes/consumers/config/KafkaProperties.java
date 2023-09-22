package pl.allegro.tech.hermes.consumers.config;

import pl.allegro.tech.hermes.common.config.KafkaAuthorizationProperties;
import pl.allegro.tech.hermes.common.kafka.KafkaParameters;

public class KafkaProperties implements KafkaParameters {

    private KafkaAuthorizationProperties authorization = new KafkaAuthorizationProperties();

    private String datacenter = "dc";

    private String clusterName = "primary";

    private String brokerList = "localhost:9092";

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

    @Override
    public String getJaasConfig() {
        if (authorization.getJaasConfig() != null) {
            return authorization.getJaasConfig();
        }
        return authorization.getLoginModule() + " required\n"
                + "username=\"" + authorization.getUsername() + "\"\n"
                + "password=\"" + authorization.getPassword() + "\";";
    }
}
