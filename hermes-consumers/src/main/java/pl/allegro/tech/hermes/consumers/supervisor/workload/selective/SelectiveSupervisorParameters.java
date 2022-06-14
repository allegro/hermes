package pl.allegro.tech.hermes.consumers.supervisor.workload.selective;

public class SelectiveSupervisorParameters {

    private final int rebalanceInterval;

    private final int consumersPerSubscription;

    private final int maxSubscriptionsPerConsumer;

    private final String nodeId;

    private final boolean autoRebalance;

    public int getRebalanceInterval() {
        return rebalanceInterval;
    }

    public int getConsumersPerSubscription() {
        return consumersPerSubscription;
    }

    public int getMaxSubscriptionsPerConsumer() {
        return maxSubscriptionsPerConsumer;
    }

    public String getNodeId() {
        return nodeId;
    }

    public boolean isAutoRebalance() {
        return autoRebalance;
    }

    public SelectiveSupervisorParameters(int rebalanceInterval, int consumersPerSubscription, int maxSubscriptionsPerConsumer, String nodeId, boolean autoRebalance) {
        this.rebalanceInterval = rebalanceInterval;
        this.consumersPerSubscription = consumersPerSubscription;
        this.maxSubscriptionsPerConsumer = maxSubscriptionsPerConsumer;
        this.nodeId = nodeId;
        this.autoRebalance = autoRebalance;
    }
}
