package pl.allegro.tech.hermes.management.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import pl.allegro.tech.hermes.api.Topic;

@ConfigurationProperties(prefix = "topic")
public class TopicProperties {

    private int replicationFactor = 1;

    private int partitions = 10;

    private boolean allowRemoval = false;

    private boolean uncleanLeaderElectionEnabled = false;

    private Topic.ContentType defaultContentType = Topic.ContentType.JSON;

    public int getReplicationFactor() {
        return replicationFactor;
    }

    public void setReplicationFactor(int replicationFactor) {
        this.replicationFactor = replicationFactor;
    }

    public int getPartitions() {
        return partitions;
    }

    public void setPartitions(int partitions) {
        this.partitions = partitions;
    }

    public boolean isAllowRemoval() {
        return allowRemoval;
    }

    public void setAllowRemoval(boolean allowRemoval) {
        this.allowRemoval = allowRemoval;
    }

    public boolean isUncleanLeaderElectionEnabled() {
        return uncleanLeaderElectionEnabled;
    }

    public void setUncleanLeaderElectionEnabled(boolean uncleanLeaderElectionEnabled) {
        this.uncleanLeaderElectionEnabled = uncleanLeaderElectionEnabled;
    }

    public Topic.ContentType getDefaultContentType() {
        return defaultContentType;
    }

    public void setDefaultContentType(Topic.ContentType defaultContentType) {
        this.defaultContentType = defaultContentType;
    }
}
