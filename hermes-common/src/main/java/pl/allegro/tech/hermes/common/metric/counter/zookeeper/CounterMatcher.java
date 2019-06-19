package pl.allegro.tech.hermes.common.metric.counter.zookeeper;

import java.util.Optional;

class CounterMatcher {

    private static final int TOPIC_METRICS_PARTS = 3;
    private static final int SUBSCRIPTION_METRIC_PARTS = 4;

    private final String counterName;
    private String topicName;
    private Optional<String> subscription;
    private int metricParts;

    public CounterMatcher(String counterName) {
        this.counterName = counterName;
        parseCounter(counterName);
    }

    private void parseCounter(String counterName) {
        String[] splitted = counterName.split("\\.");
        metricParts = splitted.length;
        if (isTopicPublished() || isTopicThroughput()) {
            topicName = splitted[splitted.length - 2] + "." + splitted[splitted.length - 1];
            subscription = Optional.empty();
        } else if (
                isSubscriptionDelivered()
                        || isSubscriptionThroughput()
                        || isSubscriptionDiscarded()
                        || isSubscriptionInflight()
                        || isSubscriptionFiltered()
                ) {
            subscription = Optional.of(splitted[splitted.length - 1]);
            topicName = splitted[splitted.length - 3] + "." + splitted[splitted.length - 2];
        }
    }

    public boolean isTopicPublished() {
        return counterName.startsWith("published.");
    }

    public boolean isTopicThroughput() {
        return counterName.startsWith("throughput.") && metricParts == TOPIC_METRICS_PARTS;
    }

    public boolean isSubscriptionThroughput() {
        return counterName.startsWith("throughput.") && metricParts == SUBSCRIPTION_METRIC_PARTS;
    }

    public boolean isSubscriptionDelivered() {
        return counterName.startsWith("delivered.");
    }

    public boolean isSubscriptionDiscarded() {
        return counterName.startsWith("discarded.");
    }

    public boolean isSubscriptionInflight() {
        return counterName.startsWith("inflight.");
    }

    public boolean isSubscriptionFiltered() {
        return counterName.startsWith("filtered.");
    }

    public String getTopicName() {
        return topicName;
    }

    public String getSubscriptionName() {
        return subscription.orElse("");
    }
}
