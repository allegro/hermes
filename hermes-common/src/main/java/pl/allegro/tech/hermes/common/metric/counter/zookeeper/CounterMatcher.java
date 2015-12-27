package pl.allegro.tech.hermes.common.metric.counter.zookeeper;

import java.util.Optional;

import static pl.allegro.tech.hermes.common.metric.HermesMetrics.escapeDots;

class CounterMatcher {

    private final String counterName;
    private String topicName;
    private Optional<String> subscription;

    public CounterMatcher(String counterName) {
        this.counterName = counterName;
        parseCounter(counterName);
    }

    private void parseCounter(String counterName) {
        String[] splitted = counterName.split("\\.");
        if (isTopicPublished()) {
            topicName = splitted[splitted.length-2] + "." + splitted[splitted.length-1];
            subscription = Optional.empty();
        } else if (isSubscriptionDelivered() || isSubscriptionDiscarded() || isSubscriptionInflight()) {
            subscription = Optional.of(splitted[splitted.length -1]);
            topicName = splitted[splitted.length-3] + "." + splitted[splitted.length-2];
        }
    }

    public boolean isTopicPublished() {
        return counterName.contains("published.");
    }

    public boolean isSubscriptionDelivered() {
        return counterName.contains("delivered.");
    }

    public boolean isSubscriptionDiscarded() {
        return counterName.contains("discarded");
    }

    public boolean isSubscriptionInflight() {
        return counterName.contains("inflight.");
    }

    public String getTopicName() {
        return topicName;
    }

    public String getSubscriptionName() {
        return subscription.orElse("");
    }
}
