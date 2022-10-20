package pl.allegro.tech.hermes.metrics;

import java.util.Optional;

public class PathContext {

    private final Optional<String> group;
    private final Optional<String> topic;
    private final Optional<String> subscription;
    private final Optional<String> kafkaTopic;
    private final Optional<Integer> partition;
    private final Optional<String> kafkaCluster;
    private final Optional<Integer> httpCode;
    private final Optional<String> httpCodeFamily;
    private final Optional<String> executorName;
    private final Optional<String> oAuthProviderName;
    private final Optional<String> schemaRepoType;

    private PathContext(Optional<String> group,
                        Optional<String> topic,
                        Optional<String> subscription,
                        Optional<String> kafkaTopic,
                        Optional<Integer> partition,
                        Optional<String> kafkaCluster,
                        Optional<Integer> httpCode,
                        Optional<String> httpCodeFamily,
                        Optional<String> executorName,
                        Optional<String> oAuthProviderName,
                        Optional<String> schemaRepoType)  {
        this.group = group;
        this.topic = topic;
        this.subscription = subscription;
        this.kafkaTopic = kafkaTopic;
        this.partition = partition;
        this.kafkaCluster = kafkaCluster;
        this.httpCode = httpCode;
        this.httpCodeFamily = httpCodeFamily;
        this.executorName = executorName;
        this.oAuthProviderName = oAuthProviderName;
        this.schemaRepoType = schemaRepoType;
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

    public Optional<String> getKafkaTopic() {
        return kafkaTopic;
    }

    public Optional<Integer> getPartition() {
        return partition;
    }

    public Optional<String> getKafkaCluster() {
        return kafkaCluster;
    }

    public Optional<Integer> getHttpCode() {
        return httpCode;
    }

    public Optional<String> getHttpCodeFamily() {
        return httpCodeFamily;
    }

    public Optional<String> getExecutorName() {
        return executorName;
    }

    public Optional<String> getoAuthProviderName() {
        return oAuthProviderName;
    }

    public Optional<String> getSchemaRepoType() {
        return schemaRepoType;
    }

    public static Builder pathContext() {
        return new Builder();
    }

    public static class Builder {

        private Optional<String> group = Optional.empty();
        private Optional<String> topic = Optional.empty();
        private Optional<String> subscription = Optional.empty();
        private Optional<String> kafkaTopic = Optional.empty();
        private Optional<Integer> partition = Optional.empty();
        private Optional<String> kafkaCluster = Optional.empty();
        private Optional<Integer> httpCode = Optional.empty();
        private Optional<String> httpCodeFamily = Optional.empty();
        private Optional<String> executorName = Optional.empty();
        private Optional<String> oAuthProviderName = Optional.empty();
        private Optional<String> schemaRepoType = Optional.empty();

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

        public Builder withKafkaTopic(String kafkaTopic) {
            this.kafkaTopic = Optional.of(kafkaTopic);
            return this;
        }

        public Builder withPartition(int partition) {
            this.partition = Optional.of(partition);
            return this;
        }

        public Builder withKafkaCluster(String kafkaCluster) {
            this.kafkaCluster = Optional.of(kafkaCluster);
            return this;
        }

        public Builder withHttpCode(int httpCode) {
            this.httpCode = Optional.of(httpCode);
            return this;
        }

        public Builder withHttpCodeFamily(String httpCodeFamily) {
            this.httpCodeFamily = Optional.of(httpCodeFamily);
            return this;
        }

        public Builder withExecutorName(String executorName) {
            this.executorName = Optional.of(executorName);
            return this;
        }

        public Builder withOAuthProvider(String oAuthProviderName) {
            this.oAuthProviderName = Optional.of(oAuthProviderName);
            return this;
        }

        public Builder withSchemaRepoType(String schemaRepoType) {
            this.schemaRepoType = Optional.of(schemaRepoType);
            return this;
        }

        public PathContext build() {
            return new PathContext(group, topic, subscription, kafkaTopic, partition, kafkaCluster,
                    httpCode, httpCodeFamily, executorName, oAuthProviderName, schemaRepoType);
        }
    }
}
