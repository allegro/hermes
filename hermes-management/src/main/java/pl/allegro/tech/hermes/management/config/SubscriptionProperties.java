package pl.allegro.tech.hermes.management.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties("subscription")
public class SubscriptionProperties {

    private List<String> additionalEndpointProtocols = new ArrayList<>();

    private int intervalBetweenCheckinIfOffsetsMovedInMillis = 50;

    private int offsetsMovedTimeoutInSeconds = 30;

    public List<String> getAdditionalEndpointProtocols() {
        return additionalEndpointProtocols;
    }

    public void setAdditionalEndpointProtocols(List<String> additionalEndpointProtocols) {
        this.additionalEndpointProtocols = additionalEndpointProtocols;
    }

    public int getIntervalBetweenCheckinIfOffsetsMovedInMillis() {
        return intervalBetweenCheckinIfOffsetsMovedInMillis;
    }

    public void setIntervalBetweenCheckinIfOffsetsMovedInMillis(int intervalBetweenCheckinIfOffsetsMovedInMillis) {
        this.intervalBetweenCheckinIfOffsetsMovedInMillis = intervalBetweenCheckinIfOffsetsMovedInMillis;
    }

    public int getOffsetsMovedTimeoutInSeconds() {
        return offsetsMovedTimeoutInSeconds;
    }

    public void setOffsetsMovedTimeoutInSeconds(int offsetsMovedTimeoutInSeconds) {
        this.offsetsMovedTimeoutInSeconds = offsetsMovedTimeoutInSeconds;
    }
}
