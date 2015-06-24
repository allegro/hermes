package pl.allegro.tech.hermes.integration.helper;

import com.jayway.awaitility.Duration;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import org.apache.curator.framework.CuratorFramework;
import pl.allegro.tech.hermes.api.PublishedMessageTraceStatus;
import pl.allegro.tech.hermes.api.SentMessageTraceStatus;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;
import pl.allegro.tech.hermes.test.helper.endpoint.HermesEndpoints;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.jayway.awaitility.Awaitility.await;
import static pl.allegro.tech.hermes.integration.helper.TimeoutAdjuster.adjust;

public class Waiter {

    private final HermesEndpoints endpoints;

    private final CuratorFramework zookeeper;

    private final CuratorFramework kafkaZookeeper;

    private final ZookeeperPaths zookeeperPaths = new ZookeeperPaths(Configs.ZOOKEEPER_ROOT.getDefaultValue().toString());

    public Waiter(HermesEndpoints endpoints, CuratorFramework zookeeper, CuratorFramework kafkaZookeeper) {
        this.endpoints = endpoints;
        this.zookeeper = zookeeper;
        this.kafkaZookeeper = kafkaZookeeper;
    }

    public void untilKafkaZookeeperNodeDeletion(final String path) {
        untilZookeeperNodeDeletion(path, kafkaZookeeper);
    }

    public void untilZookeeperNodeCreation(final String path) {
        untilZookeeperNodeCreation(path, zookeeper);
    }

    public void untilKafkaZookeeperNodeEmptied(final String path, int seconds) {
        untilZookeeperNodeEmptied(path, seconds, kafkaZookeeper);
    }

    public void untilTopicDetailsAreCreated(TopicName topicName) {
        untilZookeeperNodeCreation(zookeeperPaths.topicPath(topicName));
    }

    public void untilSubscriptionMetricsIsCreated(TopicName topicName, String subscriptionName) {
        untilZookeeperNodeCreation(zookeeperPaths.subscriptionMetricsPath(topicName, subscriptionName));
    }

    public void untilSubscriptionMetricsIsRemoved(TopicName topicName, String subscriptionName) {
        untilZookeeperNodeDeletion(zookeeperPaths.subscriptionMetricsPath(topicName, subscriptionName), zookeeper);
    }

    public void untilSubscriptionIsDeactivated(String group, String topic, String subscription) {
        untilKafkaZookeeperNodeEmptied(subscriptionConsumerPath(group, topic, subscription), 60);
    }

    public void untilSubscriptionEndsReiteration(TopicName topicName, String subscription) {
        untilSubscriptionEndsReiteration(topicName.getGroupName(), topicName.getName(), subscription);
    }

    public void untilSubscriptionEndsReiteration(final String group, final String topic, final String subscription) {
        await().atMost(adjust(30), TimeUnit.SECONDS).until(() -> {
            Subscription.State state = endpoints.subscription().get(group + "." + topic, subscription).getState();
            return state == Subscription.State.ACTIVE;
        });
    }

    public void untilAllOffsetsEqual(final String group, final String topic, final String subscription, final int offset) {
        await().atMost(adjust(30), TimeUnit.SECONDS).until(() -> {
            List<String> partitions = zookeeper.getChildren().forPath(subscriptionOffsetPath(group, topic, subscription));
            for (String partition: partitions) {
                Long currentOffset = Long.valueOf(new String(
                        zookeeper.getData().forPath(subscriptionOffsetPath(group, topic, subscription) + "/" + partition)));
                if (currentOffset != offset) {
                    return false;
                }
            }
            return true;
        });
    }

    public void untilConsumersRebalance(final String group, final String topic, final String subscription, final int consumerCount) {
        await().atMost(Duration.ONE_MINUTE).until(() -> {
            List<String> children = zookeeper.getChildren().forPath(subscriptionIdsPath(group, topic, subscription));
            return children != null && children.size() == consumerCount;
        });
    }

    public void untilConsumersStop() {
        sleep(3);
    }

    public void untilConsumersStart() {
        sleep(3);
    }

    public void untilConsumerCommitsOffset() {
        sleep(4);
    }

    public void untilPublishedMetricsPropagation() {
        sleep(2);
    }

    public void untilMessageDiscarded() {
        sleep(5);
    }

    public void untilMessageTraceLogged(final DBCollection collection, final PublishedMessageTraceStatus status) {
        await().atMost(adjust(new Duration(30, TimeUnit.SECONDS))).until(() -> collection.find(new BasicDBObject("status", status.toString())).count() > 0);
    }

    public void untilMessageTraceLogged(final DBCollection collection, final SentMessageTraceStatus status) {
        await().atMost(adjust(new Duration(30, TimeUnit.SECONDS))).until(() -> collection.find(new BasicDBObject("status", status.toString())).count() > 0);
    }

    public void untilMessageIdLogged(final DBCollection collection, final String messageId) {
        await().atMost(adjust(new Duration(30, TimeUnit.SECONDS))).until(() -> collection.find(new BasicDBObject("messageId", messageId)).count() > 0);
    }

    public void untilReceivedAnyMessage(final DBCollection collection) {
        await().atMost(adjust(new Duration(30, TimeUnit.SECONDS))).until(() -> collection.find().count() > 0);
    }

    public void untilSubscriptionUpdated() {
        sleep(2);
    }

    private String subscriptionConsumerPath(String group, String topic, String subscription) {
        TopicName topicName = new TopicName(group, topic);
        return "/consumers/" + Subscription.getId(topicName, subscription) + "/owners/" + topicName.qualifiedName();
    }

    private String subscriptionIdsPath(String group, String topic, String subscription) {
        TopicName topicName = new TopicName(group, topic);
        return "/consumers/" + Subscription.getId(topicName, subscription) + "/ids";
    }

    private String subscriptionOffsetPath(String group, String topic, String subscription) {
        TopicName topicName = new TopicName(group, topic);
        return "/consumers/" + Subscription.getId(topicName, subscription) + "/offsets/" + topicName.qualifiedName();
    }

    private void sleep(int seconds) {
        try {
            Thread.sleep(adjust(seconds * 1000));
        } catch (InterruptedException exception) {
            throw new RuntimeException("Who dares to interrupt me?", exception);
        }
    }

    private void untilZookeeperNodeEmptied(final String path, int seconds, final CuratorFramework zookeeper) {
        await().atMost(adjust(seconds), TimeUnit.SECONDS).until(() -> {
            List<String> children = zookeeper.getChildren().forPath(path);
            return children == null || children.isEmpty();
        });
    }

    private void untilZookeeperNodeCreation(final String path, final CuratorFramework zookeeper) {
        await().atMost(adjust(20), TimeUnit.SECONDS).until(() -> zookeeper.checkExists().forPath(path) != null);
    }

    private void untilZookeeperNodeDeletion(final String path, final CuratorFramework zookeeper) {
        await().atMost(adjust(Duration.FIVE_SECONDS)).until(() -> zookeeper.checkExists().forPath(path) == null);
    }

    public void untilTopicUpdated() {
        sleep(2);
    }
}
