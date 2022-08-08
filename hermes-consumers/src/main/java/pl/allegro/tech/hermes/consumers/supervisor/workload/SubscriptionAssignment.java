package pl.allegro.tech.hermes.consumers.supervisor.workload;

import pl.allegro.tech.hermes.api.SubscriptionName;

import java.util.Objects;

public class SubscriptionAssignment {
    private final String consumerNodeId;
    private final SubscriptionName subscriptionName;

    public SubscriptionAssignment(String consumerNodeId, SubscriptionName subscriptionName) {
        this.consumerNodeId = consumerNodeId;
        this.subscriptionName = subscriptionName;
    }

    public String getConsumerNodeId() {
        return consumerNodeId;
    }

    public SubscriptionName getSubscriptionName() {
        return subscriptionName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SubscriptionAssignment that = (SubscriptionAssignment) o;
        return Objects.equals(consumerNodeId, that.consumerNodeId)
                && Objects.equals(subscriptionName, that.subscriptionName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(consumerNodeId, subscriptionName);
    }
}
