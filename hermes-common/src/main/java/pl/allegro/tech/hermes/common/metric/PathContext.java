package pl.allegro.tech.hermes.common.metric;

import java.util.Optional;

public class PathContext {

    private final Optional<String> group;
    private final Optional<String> topic;
    private final Optional<String> subscription;
    private final Optional<Integer> partition;
    private final Optional<Integer> httpCode;

    private PathContext(Optional<String> group,
                        Optional<String> topic,
                        Optional<String> subscription,
                        Optional<Integer> partition,
                        Optional<Integer> httpCode) {
        this.group = group;
        this.topic = topic;
        this.subscription = subscription;
        this.partition = partition;
        this.httpCode = httpCode;
    }

    public Optional<String> getGroup() {
        return group;
    }

    public Optional<String> getTopic() {
        return topic;
    }

    public Optional<String> getSubscription() {
        return subscription;
    }

    public Optional<Integer> getPartition() {
        return partition;
    }

    public Optional<Integer> getHttpCode() {
        return httpCode;
    }

    public static Builder pathContext() {
        return new Builder();
    }

    public static class Builder {

        private Optional<String> group = Optional.empty();
        private Optional<String> topic = Optional.empty();
        private Optional<String> subscription = Optional.empty();
        private Optional<Integer> partition = Optional.empty();
        private Optional<Integer> httpCode = Optional.empty();

        public Builder withGroup(String group) {
            this.group = Optional.of(group);
            return this;
        }

        public Builder withTopic(String topic) {
            this.topic = Optional.of(topic);
            return this;
        }

        public Builder withSubscription(String subscription) {
            this.subscription = Optional.of(subscription);
            return this;
        }

        public Builder withPartition(int partition) {
            this.partition = Optional.of(partition);
            return this;
        }

        public Builder withHttpCode(int httpCode) {
            this.httpCode = Optional.of(httpCode);
            return this;
        }

        public PathContext build() {
            return new PathContext(group, topic, subscription, partition, httpCode);
        }
    }
}
