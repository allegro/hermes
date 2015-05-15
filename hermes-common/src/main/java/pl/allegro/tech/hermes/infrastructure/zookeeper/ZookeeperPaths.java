package pl.allegro.tech.hermes.infrastructure.zookeeper;

import com.google.common.base.Joiner;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.TopicName;

public class ZookeeperPaths {

    public static final String TOPICS_PATH = "topics";
    public static final String GROUPS_PATH = "groups";
    public static final String SUBSCRIPTIONS_PATH = "subscriptions";
    public static final String URL_SEPARATOR = "/";
    public static final String CONSUMERS_PATH = "consumers";
    public static final String METRICS_PATH = "metrics";

    private final String basePath;

    public ZookeeperPaths(String basePath) {
        this.basePath = basePath;
    }

    public String basePath() {
        return basePath;
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

    public String offsetPath(TopicName topicName, String subscriptionName, String brokersClusterName, int partitionId) {
        return Joiner.on(URL_SEPARATOR).join(offsetsPath(topicName, subscriptionName, brokersClusterName), partitionId);
    }

    public String offsetsPath(TopicName topicName, String subscriptionName, String brokersClusterName) {
        return Joiner.on(URL_SEPARATOR).join(subscriptionPath(topicName, subscriptionName), "offset", brokersClusterName);
    }

    public String consumersPath() {
        return Joiner.on(URL_SEPARATOR).join(basePath, CONSUMERS_PATH);
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
