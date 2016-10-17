package pl.allegro.tech.hermes.management.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "topic")
public class TopicProperties {

    private int replicationFactor = 1;

    private int partitions = 10;

    private boolean allowRemoval = false;

    private boolean removeSchema = false;

    private boolean uncleanLeaderElectionEnabled = false;

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

    public boolean isRemoveSchema() {
        return removeSchema;
    }

    public void setRemoveSchema(boolean removeSchema) {
        this.removeSchema = removeSchema;
    }

    public boolean isUncleanLeaderElectionEnabled() {
        return uncleanLeaderElectionEnabled;
    }

    public void setUncleanLeaderElectionEnabled(boolean uncleanLeaderElectionEnabled) {
        this.uncleanLeaderElectionEnabled = uncleanLeaderElectionEnabled;
    }
}
