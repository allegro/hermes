package pl.allegro.tech.hermes.consumers.consumer.offset;

import java.util.HashSet;
import java.util.Set;

public class FailedToCommitOffsets {

    private final Set<SubscriptionPartitionOffset> offsets = new HashSet<>();

    public FailedToCommitOffsets() {
    }

    public void add(Set<SubscriptionPartitionOffset> failedOffsets) {
        this.offsets.addAll(failedOffsets);
    }

    public void add(SubscriptionPartitionOffset failedOffset) {
        this.offsets.add(failedOffset);
    }

    public boolean hasFailed() {
        return !offsets.isEmpty();
    }

    public Set<SubscriptionPartitionOffset> failedOffsets() {
        return offsets;
    }
}
