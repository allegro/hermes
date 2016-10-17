package pl.allegro.tech.hermes.integration.helper;

import com.jayway.awaitility.Duration;
import com.jayway.awaitility.core.ConditionFactory;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import kafka.api.GroupCoordinatorRequest;
import kafka.api.GroupCoordinatorResponse;
import kafka.common.ErrorMapping;
import kafka.network.BlockingChannel;
import org.apache.curator.framework.CuratorFramework;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.ContentType;
import pl.allegro.tech.hermes.api.EndpointAddress;
import pl.allegro.tech.hermes.api.PublishedMessageTraceStatus;
import pl.allegro.tech.hermes.api.SentMessageTraceStatus;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.common.kafka.JsonToAvroMigrationKafkaNamesMapper;
import pl.allegro.tech.hermes.common.kafka.KafkaNamesMapper;
import pl.allegro.tech.hermes.common.kafka.KafkaTopic;
import pl.allegro.tech.hermes.common.kafka.KafkaZookeeperPaths;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;
import pl.allegro.tech.hermes.test.helper.endpoint.HermesEndpoints;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.jayway.awaitility.Awaitility.waitAtMost;
import static pl.allegro.tech.hermes.test.helper.endpoint.TimeoutAdjuster.adjust;

public class Waiter extends pl.allegro.tech.hermes.test.helper.endpoint.Waiter {

    private static final Logger logger = LoggerFactory.getLogger(Waiter.class);

    private final HermesEndpoints endpoints;

    private final CuratorFramework zookeeper;

    private final CuratorFramework kafkaZookeeper;

    private final ZookeeperPaths zookeeperPaths = new ZookeeperPaths(Configs.ZOOKEEPER_ROOT.getDefaultValue());

    private final KafkaNamesMapper kafkaNamesMapper;

    public Waiter(HermesEndpoints endpoints, CuratorFramework zookeeper, CuratorFramework kafkaZookeeper, String kafkaNamespace) {
        super(endpoints);
        this.endpoints = endpoints;
        this.zookeeper = zookeeper;
        this.kafkaZookeeper = kafkaZookeeper;
        this.kafkaNamesMapper = new JsonToAvroMigrationKafkaNamesMapper(kafkaNamespace);
    }

    public void untilKafkaZookeeperNodeDeletion(final String path) {
        untilZookeeperNodeDeletion(path, kafkaZookeeper);
    }

    public void untilHermesZookeeperNodeCreation(final String path) {
        untilZookeeperNodeCreation(path, zookeeper);
    }

    public void untilHermesZookeeperNodeDeletion(final String path) {
        untilZookeeperNodeDeletion(path, zookeeper);
    }

    public void untilKafkaZookeeperNodeEmptied(final String path, int seconds) {
        untilZookeeperNodeEmptied(path, seconds, kafkaZookeeper);
    }

    public void untilTopicDetailsAreCreated(TopicName topicName) {
        untilHermesZookeeperNodeCreation(zookeeperPaths.topicPath(topicName));
    }

    public void untilSubscriptionMetricsIsCreated(TopicName topicName, String subscriptionName) {
        untilHermesZookeeperNodeCreation(zookeeperPaths.subscriptionMetricsPath(topicName, subscriptionName));
    }

    public void untilSubscriptionMetricsIsRemoved(TopicName topicName, String subscriptionName) {
        untilZookeeperNodeDeletion(zookeeperPaths.subscriptionMetricsPath(topicName, subscriptionName), zookeeper);
    }

    public void untilSubscriptionIsActivated(Topic topic, String subscription) {
        untilSubscriptionHasState(topic, subscription, Subscription.State.ACTIVE);

        waitAtMost(adjust(Duration.TEN_SECONDS)).until(() -> {
            return endpoints.consumer().listSubscriptions().contains(topic.getQualifiedName() + "$" + subscription);
        });
    }

    public void untilSubscriptionIsSuspended(Topic topic, String subscription) {
        untilSubscriptionHasState(topic, subscription, Subscription.State.SUSPENDED);

        waitAtMost(adjust(Duration.TEN_SECONDS)).until(() -> {
            return !endpoints.consumer().listSubscriptions().contains(topic.getQualifiedName() + "$" + subscription);
        });
    }

    private void untilSubscriptionHasState(Topic topic, String subscription, Subscription.State expected) {
        waitAtMost(adjust(Duration.TWO_MINUTES)).until(() -> {
            Subscription.State actual = endpoints.subscription().get(topic.getQualifiedName(), subscription).getState();
            logger.info("Expecting {} subscription state. Actual {}", expected, actual);
            return expected == actual;
        });
    }

    public void untilSubscriptionContentTypeChanged(Topic topic, String subscription, ContentType expected) {
        waitAtMost(adjust(Duration.TWO_MINUTES)).until(() -> {
            ContentType actual = endpoints.subscription().get(topic.getQualifiedName(), subscription).getContentType();
            logger.info("Expecting {} subscription endpoint address. Actual {}", expected, actual);
            return expected.equals(actual);
        });
    }

    public void untilSubscriptionEndpointAddressChanged(Topic topic, String subscription, EndpointAddress expected) {
        waitAtMost(adjust(Duration.TWO_MINUTES)).until(() -> {
            EndpointAddress actual = endpoints.subscription().get(topic.getQualifiedName(), subscription).getEndpoint();
            logger.info("Expecting {} subscription endpoint address. Actual {}", expected, actual);
            return expected.equals(actual);
        });
    }

    public void untilSubscriptionEndsReiteration(Topic topic, String subscription) {
        untilSubscriptionHasState(topic, subscription, Subscription.State.ACTIVE);
    }

    public void untilTopicRemovedInKafka(Topic topic) {
        kafkaNamesMapper.toKafkaTopics(topic).forEach(k ->
                        untilKafkaZookeeperNodeDeletion(KafkaZookeeperPaths.topicPath(k.name()))
        );
    }

    public void untilTopicBlacklisted(String qualifiedTopicName) {
        untilHermesZookeeperNodeCreation(zookeeperPaths.blacklistedTopicPath(qualifiedTopicName));
    }

    public void untilTopicUnblacklisted(String qualifiedTopicName) {
        untilHermesZookeeperNodeDeletion(zookeeperPaths.blacklistedTopicPath(qualifiedTopicName));
    }

    public void untilAllOffsetsEqualOnPrimaryKafkaTopic(Topic topic, final String subscription, final int offset) {
        KafkaTopic primaryKafkaTopic = kafkaNamesMapper.toKafkaTopics(topic).getPrimary();

        waitAtMost(adjust(30), TimeUnit.SECONDS).until(() -> {
            List<String> partitions = zookeeper.getChildren().forPath(subscriptionOffsetPath(topic, primaryKafkaTopic, subscription));
            for (String partition : partitions) {
                Long currentOffset = Long.valueOf(new String(
                        zookeeper.getData().forPath(subscriptionOffsetPath(topic, primaryKafkaTopic, subscription) + "/" + partition)));
                if (currentOffset != offset) {
                    return false;
                }
            }
            return true;
        });
    }

    public void untilConsumersRebalance(Topic topic, final String subscription, final int consumerCount) {
        waitAtMost(adjust(Duration.ONE_MINUTE)).until(() -> {
            List<String> children = zookeeper.getChildren().forPath(subscriptionIdsPath(topic, subscription));
            return children != null && children.size() == consumerCount;
        });
    }

    public void untilConsumersStop() {
        sleep(3);
    }

    public void untilConsumersStart() {
        sleep(3);
    }

    public void untilConsumersUpdateSubscription() {
        sleep(1);
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
        waitAtMost(adjust(Duration.ONE_MINUTE)).until(() -> collection.find(new BasicDBObject("status", status.toString())).count() > 0);
    }

    public void untilMessageTraceLogged(final DBCollection collection, final SentMessageTraceStatus status) {
        waitAtMost(adjust(Duration.ONE_MINUTE)).until(() -> collection.find(new BasicDBObject("status", status.toString())).count() > 0);
    }

    public void untilMessageIdLogged(final DBCollection collection, final String messageId) {
        waitAtMost(adjust(Duration.ONE_MINUTE)).until(() -> collection.find(new BasicDBObject("messageId", messageId)).count() > 0);
    }

    public void untilReceivedAnyMessage(final DBCollection collection) {
        waitAtMost(adjust(Duration.ONE_MINUTE)).until(() -> collection.find().count() > 0);
    }

    private String subscriptionConsumerPath(Topic topic, KafkaTopic kafkaTopic, String subscription) {
        return KafkaZookeeperPaths.ownersPath(kafkaNamesMapper.toConsumerGroupId(new SubscriptionName(subscription, topic.getName())),
                kafkaTopic.name());
    }

    private String subscriptionIdsPath(Topic topic, String subscription) {
        return KafkaZookeeperPaths.idsPath(kafkaNamesMapper.toConsumerGroupId(new SubscriptionName(subscription, topic.getName())));
    }

    private String subscriptionOffsetPath(Topic topic, KafkaTopic kafkaTopic, String subscription) {
        return KafkaZookeeperPaths.offsetsPath(kafkaNamesMapper.toConsumerGroupId(new SubscriptionName(subscription, topic.getName())),
                kafkaTopic.name());
    }

    private void sleep(int seconds) {
        try {
            Thread.sleep(adjust(seconds * 1000));
        } catch (InterruptedException exception) {
            throw new RuntimeException("Who dares to interrupt me?", exception);
        }
    }

    private void untilZookeeperNodeEmptied(final String path, int seconds, final CuratorFramework zookeeper) {
        waitAtMost(adjust(seconds), TimeUnit.SECONDS).until(() -> {
            List<String> children = zookeeper.getChildren().forPath(path);
            return children == null || children.isEmpty();
        });
    }

    private void untilZookeeperNodeCreation(final String path, final CuratorFramework zookeeper) {
        waitAtMost(adjust(60), TimeUnit.SECONDS).until(() -> zookeeper.checkExists().forPath(path) != null);
    }

    private void untilZookeeperNodeDeletion(final String path, final CuratorFramework zookeeper) {
        waitAtMost(adjust(Duration.FIVE_SECONDS)).until(() -> zookeeper.checkExists().forPath(path) == null);
    }

    public void waitUntilConsumerMetadataAvailable(Subscription subscription, String host, int port) {
        BlockingChannel channel = createBlockingChannel(host, port);
        channel.connect();

        waitAtMost(adjust((Duration.ONE_MINUTE))).until(() -> {
            channel.send(new GroupCoordinatorRequest(kafkaNamesMapper.toConsumerGroupId(subscription.getQualifiedName()).asString(),
                    GroupCoordinatorRequest.CurrentVersion(), 0, "0"));
            GroupCoordinatorResponse metadataResponse = GroupCoordinatorResponse.readFrom(channel.receive().payload());
            return metadataResponse.errorCode() == ErrorMapping.NoError();
        });

        channel.disconnect();
    }

    private BlockingChannel createBlockingChannel(String host, int port) {
        return new BlockingChannel(host, port,
                BlockingChannel.UseDefaultBufferSize(),
                BlockingChannel.UseDefaultBufferSize(),
                (int) adjust(Duration.TEN_SECONDS).getValueInMS());
    }

    public ConditionFactory awaitAtMost(Duration duration) {
        return waitAtMost(adjust(duration));
    }

    public void until(Runnable runnable) {
        awaitAtMost(adjust(new Duration(30, TimeUnit.SECONDS))).until(runnable);
    }
}
