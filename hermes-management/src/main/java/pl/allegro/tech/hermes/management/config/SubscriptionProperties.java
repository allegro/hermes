package pl.allegro.tech.hermes.management.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties("subscription")
public class SubscriptionProperties {

    private List<String> additionalEndpointProtocols = new ArrayList<>();

    public List<String> getAdditionalEndpointProtocols() {
        return additionalEndpointProtocols;
    }

    public void setAdditionalEndpointProtocols(List<String> additionalEndpointProtocols) {
        this.additionalEndpointProtocols = additionalEndpointProtocols;
    }
}
