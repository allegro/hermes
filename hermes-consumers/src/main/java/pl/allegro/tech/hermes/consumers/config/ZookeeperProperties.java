package pl.allegro.tech.hermes.consumers.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "zookeeper")
public class ZookeeperProperties {

    private ZookeeperAuthorizationProperties authorization;

    public ZookeeperAuthorizationProperties getAuthorization() {
        return authorization;
    }

    public void setAuthorization(ZookeeperAuthorizationProperties authorization) {
        this.authorization = authorization;
    }
}
