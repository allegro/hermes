package pl.allegro.tech.hermes.integration.helper;

import com.jayway.awaitility.Duration;
import com.jayway.awaitility.core.ConditionFactory;
import org.apache.curator.framework.CuratorFramework;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.ContentType;
import pl.allegro.tech.hermes.api.EndpointAddress;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.common.kafka.JsonToAvroMigrationKafkaNamesMapper;
import pl.allegro.tech.hermes.common.kafka.KafkaNamesMapper;
import pl.allegro.tech.hermes.consumers.config.ZookeeperProperties;
import pl.allegro.tech.hermes.consumers.supervisor.process.RunningSubscriptionStatus;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;
import pl.allegro.tech.hermes.test.helper.endpoint.BrokerOperations;
import pl.allegro.tech.hermes.test.helper.endpoint.HermesEndpoints;

import java.time.Clock;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import static com.jayway.awaitility.Awaitility.waitAtMost;
import static pl.allegro.tech.hermes.consumers.supervisor.process.Signal.SignalType.COMMIT;
import static pl.allegro.tech.hermes.consumers.supervisor.process.Signal.SignalType.START;
import static pl.allegro.tech.hermes.consumers.supervisor.process.Signal.SignalType.UPDATE_SUBSCRIPTION;
import static pl.allegro.tech.hermes.consumers.supervisor.process.Signal.SignalType.UPDATE_TOPIC;
import static pl.allegro.tech.hermes.test.helper.endpoint.TimeoutAdjuster.adjust;

public class Waiter extends pl.allegro.tech.hermes.test.helper.endpoint.Waiter {

    private static final Logger logger = LoggerFactory.getLogger(Waiter.class);

    private final HermesEndpoints endpoints;

    private final CuratorFramework zookeeper;

    private final BrokerOperations brokerOperations;

    private final String clusterName;

    private final ZookeeperPaths zookeeperPaths;

    private final KafkaNamesMapper kafkaNamesMapper;

    private final Clock clock = Clock.systemDefaultZone();

    public Waiter(HermesEndpoints endpoints, CuratorFramework zookeeper, BrokerOperations brokerOperations,
                  String clusterName, String kafkaNamespace) {
        super(endpoints);
        this.endpoints = endpoints;
        this.zookeeper = zookeeper;
        this.brokerOperations = brokerOperations;
        this.clusterName = clusterName;
        this.kafkaNamesMapper = new JsonToAvroMigrationKafkaNamesMapper(kafkaNamespace, "_");
        ZookeeperProperties zookeeperProperties = new ZookeeperProperties();
        this.zookeeperPaths = new ZookeeperPaths(zookeeperProperties.getRoot());
    }

    public void untilHermesZookeeperNodeCreation(final String path) {
        untilZookeeperNodeCreation(path, zookeeper);
    }

    public void untilHermesZookeeperNodeDeletion(final String path) {
        untilZookeeperNodeDeletion(path, zookeeper);
    }

    public void untilSubscriptionMetricsIsCreated(TopicName topicName, String subscriptionName) {
        untilHermesZookeeperNodeCreation(zookeeperPaths.subscriptionMetricsPath(topicName, subscriptionName));
    }

    public void untilSubscriptionMetricsIsRemoved(TopicName topicName, String subscriptionName) {
        untilZookeeperNodeDeletion(zookeeperPaths.subscriptionMetricsPath(topicName, subscriptionName), zookeeper);
    }

    public void untilSubscriptionIsActivated(long currentTime, Topic topic, String subscription) {
        until(Duration.TEN_SECONDS, topic, subscription, sub -> sub.getSignalTimesheet().getOrDefault(START, 0L) > currentTime);
    }

    public void untilSubscriptionIsActivated(Topic topic, String subscription) {
        untilSubscriptionIsActivated(clock.millis(), topic, subscription);
    }

    public void untilSubscriptionIsSuspended(Topic topic, String subscription) {
        untilSubscriptionHasState(topic, subscription, Subscription.State.SUSPENDED);

        waitAtMost(adjust(Duration.TEN_SECONDS)).until(() ->
                endpoints.consumer().listSubscriptions().stream()
                        .noneMatch(sub -> sub.getQualifiedName().equals(topic.getQualifiedName() + "$" + subscription)));
    }

    private void until(Duration duration, Topic topic, String subscription, Predicate<RunningSubscriptionStatus> predicate) {
        waitAtMost(adjust(duration)).until(() ->
                endpoints.consumer().listSubscriptions().stream()
                        .filter(sub -> sub.getQualifiedName().equals(topic.getQualifiedName() + "$" + subscription))
                        .anyMatch(predicate));
    }

    public void until(Runnable runnable) {
        awaitAtMost(adjust(new Duration(30, TimeUnit.SECONDS))).until(runnable);
    }

    public void until(Runnable runnable, int seconds) {
        awaitAtMost(adjust(new Duration(seconds, TimeUnit.SECONDS))).until(runnable);
    }

    public void untilTopicIsUpdatedAfter(final long currentTime, Topic topic, String subscription) {
        until(Duration.TEN_SECONDS, topic, subscription, sub ->
                sub.getSignalTimesheet().getOrDefault(UPDATE_TOPIC, 0L) > currentTime);
    }

    public void untilConsumersUpdateSubscription(final long currentTime, Topic topic, String subscription) {
        until(Duration.TEN_SECONDS, topic, subscription, sub ->
                sub.getSignalTimesheet().getOrDefault(UPDATE_SUBSCRIPTION, 0L) > currentTime);
    }

    public void untilConsumerCommitsOffset(Topic topic, String subscription) {
        long currentTime = clock.millis();
        until(Duration.TEN_SECONDS, topic, subscription, sub ->
                sub.getSignalTimesheet().getOrDefault(COMMIT, 0L) > currentTime);
    }

    private void untilSubscriptionHasState(Topic topic, String subscription, Subscription.State expected) {
        waitAtMost(adjust(Duration.TEN_SECONDS)).until(() -> {
            Subscription.State actual = endpoints.subscription().get(topic.getQualifiedName(), subscription).getState();
            logger.info("Expecting {} subscription state. Actual {}", expected, actual);
            return expected == actual;
        });
    }

    public void untilSubscriptionContentTypeChanged(Topic topic, String subscription, ContentType expected) {
        waitAtMost(adjust(Duration.TEN_SECONDS)).until(() -> {
            ContentType actual = endpoints.subscription().get(topic.getQualifiedName(), subscription).getContentType();
            logger.info("Expecting {} subscription endpoint address. Actual {}", expected, actual);
            return expected.equals(actual);
        });
    }

    public void untilSubscriptionEndpointAddressChanged(Topic topic, String subscription, EndpointAddress expected) {
        waitAtMost(adjust(Duration.TEN_SECONDS)).until(() -> {
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
                brokerOperations.topicExists(k.name().asString(), clusterName)
        );
    }

    public void untilTopicBlacklisted(String qualifiedTopicName) {
        untilHermesZookeeperNodeCreation(zookeeperPaths.blacklistedTopicPath(qualifiedTopicName));
    }

    public void untilTopicUnblacklisted(String qualifiedTopicName) {
        untilHermesZookeeperNodeDeletion(zookeeperPaths.blacklistedTopicPath(qualifiedTopicName));
    }

    private void untilZookeeperNodeCreation(final String path, final CuratorFramework zookeeper) {
        waitAtMost(adjust(60), TimeUnit.SECONDS).until(() -> zookeeper.checkExists().forPath(path) != null);
    }

    private void untilZookeeperNodeDeletion(final String path, final CuratorFramework zookeeper) {
        waitAtMost(adjust(Duration.FIVE_SECONDS)).until(() -> zookeeper.checkExists().forPath(path) == null);
    }

    public ConditionFactory awaitAtMost(Duration duration) {
        return waitAtMost(adjust(duration));
    }

}
