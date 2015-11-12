package pl.allegro.tech.hermes.infrastructure.zookeeper;

import com.google.common.base.Joiner;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.common.kafka.KafkaTopicName;

public class ZookeeperPaths {

    public static final String TOPICS_PATH = "topics";
    public static final String GROUPS_PATH = "groups";
    public static final String SUBSCRIPTIONS_PATH = "subscriptions";
    public static final String KAFKA_TOPICS_PATH = "kafka_topics";
    public static final String URL_SEPARATOR = "/";
    public static final String CONSUMERS_PATH = "consumers";
    public static final String CONSUMERS_WORKLOAD_PATH = "consumers-workload";
    public static final String METRICS_PATH = "metrics";
    public static final String ADMIN_PATH = "admin";

    private final String basePath;

    public ZookeeperPaths(String basePath) {
        this.basePath = basePath;
    }

    public String basePath() {
        return basePath;
    }

    public String adminPath() {
        return Joiner.on(URL_SEPARATOR).join(basePath, ADMIN_PATH);
    }

    public String adminOperationPath(String operation) {
        return Joiner.on(URL_SEPARATOR).join(adminPath(), operation);
    }

    public String groupsPath() {
        return Joiner.on(URL_SEPARATOR).join(basePath, GROUPS_PATH);
    }

    public String groupPath(String groupName) {
        return Joiner.on(URL_SEPARATOR).join(groupsPath(), groupName);
    }

    public String topicsPath(String groupName) {
        return Joiner.on(URL_SEPARATOR).join(groupPath(groupName), TOPICS_PATH);
    }

    public String topicMetricPath(TopicName topicName, String metricName) {
        return topicPath(topicName, "metrics", metricName);
    }

    public String subscriptionsPath(TopicName topicName) {
        return Joiner.on(URL_SEPARATOR).join(topicPath(topicName), SUBSCRIPTIONS_PATH);
    }

    public String topicPath(TopicName topicName, String... tail) {
        return Joiner.on(URL_SEPARATOR).join(topicsPath(topicName.getGroupName()), topicName.getName(), (Object[]) tail);
    }

    public String subscriptionPath(TopicName topicName, String subscriptionName, String... tail) {
        return Joiner.on(URL_SEPARATOR).join(subscriptionsPath(topicName), subscriptionName, (Object[]) tail);
    }

    public String subscriptionPath(Subscription subscription) {
        return subscriptionPath(subscription.getTopicName(), subscription.getName());
    }

    public String subscriptionMetricsPath(TopicName topicName, String subscriptionName) {
        return subscriptionPath(topicName, subscriptionName, METRICS_PATH);
    }

    public String subscriptionMetricPath(TopicName topicName, String subscriptionName, String metricName) {
        return subscriptionPath(topicName, subscriptionName, METRICS_PATH, metricName);
    }

    public String offsetPath(TopicName topicName, String subscriptionName, KafkaTopicName kafkaTopicName, String brokersClusterName, int partitionId) {
        return Joiner.on(URL_SEPARATOR).join(offsetsPath(topicName, subscriptionName, kafkaTopicName, brokersClusterName), partitionId);
    }

    public String offsetsPath(TopicName topicName, String subscriptionName, KafkaTopicName kafkaTopicName, String brokersClusterName) {
        return Joiner.on(URL_SEPARATOR).join(subscribedKafkaTopicsPath(topicName, subscriptionName), kafkaTopicName.asString(), "offset", brokersClusterName);
    }

    public String subscribedKafkaTopicsPath(TopicName topicName, String subscriptionName) {
        return Joiner.on(URL_SEPARATOR).join(subscriptionPath(topicName, subscriptionName), KAFKA_TOPICS_PATH);
    }

    public String consumersPath() {
        return Joiner.on(URL_SEPARATOR).join(basePath, CONSUMERS_PATH);
    }

    public String consumersRuntimePath(String cluster) {
        return Joiner.on(URL_SEPARATOR).join(basePath, CONSUMERS_WORKLOAD_PATH, cluster, "runtime");
    }

    public String consumersRegistryPath(String cluster) {
        return Joiner.on(URL_SEPARATOR).join(basePath, CONSUMERS_WORKLOAD_PATH, cluster, "registry");
    }

    public String inflightPath(String hostname, TopicName topicName, String subscriptionName, String metricName) {
        return Joiner.on(URL_SEPARATOR).join(
                consumersPath(),
                hostname + subscriptionMetricPathWithoutBasePath(topicName, subscriptionName, metricName)
        );
    }

    public String subscriptionMetricPathWithoutBasePath(TopicName topicName, String subscriptionName, String metricName) {
        return Joiner.on(URL_SEPARATOR).join(
                "",
                GROUPS_PATH,
                topicName.getGroupName(),
                TOPICS_PATH,
                topicName.getName(),
                SUBSCRIPTIONS_PATH,
                subscriptionName,
                METRICS_PATH,
                metricName);
    }
}
