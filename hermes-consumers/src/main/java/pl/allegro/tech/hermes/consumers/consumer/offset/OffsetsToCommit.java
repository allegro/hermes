package pl.allegro.tech.hermes.consumers.consumer.offset;

import pl.allegro.tech.hermes.api.SubscriptionName;

import java.util.HashSet;
import java.util.Set;

public class OffsetsToCommit {

    private Set<SubscriptionPartitionOffset> offsets;

    private final SubscriptionName subscriptionName;

    public OffsetsToCommit(SubscriptionName subscriptionName) {
        this.subscriptionName = subscriptionName;
    }

    public OffsetsToCommit add(SubscriptionPartitionOffset offset) {
        if (offsets == null) {
            offsets = new HashSet<>();
        }
        offsets.add(offset);
        return this;
    }

    public SubscriptionName getSubscriptionName() {
        return subscriptionName;
    }

    public Set<SubscriptionPartitionOffset> getOffsets() {
        return offsets;
    }
}
