package pl.allegro.tech.hermes.management.infrastructure.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.admin.AdminTool;
import pl.allegro.tech.hermes.common.exception.InternalProcessingException;
import pl.allegro.tech.hermes.management.domain.topic.BrokerTopicManagement;
import pl.allegro.tech.hermes.management.domain.topic.TopicContentTypeMigrationService;
import pl.allegro.tech.hermes.management.domain.topic.UnableToMoveOffsetsException;
import pl.allegro.tech.hermes.management.infrastructure.kafka.service.BrokersClusterService;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.function.Consumer;

public class MultiDCAwareService {

    private static final Logger logger = LoggerFactory.getLogger(TopicContentTypeMigrationService.class);

    private final List<BrokersClusterService> clusters;
    private final AdminTool adminTool;
    private final Clock clock;
    private final Duration intervalBetweenCheckingIfOffsetsMoved;
    private final Duration offsetsMovedTimeout;

    public MultiDCAwareService(List<BrokersClusterService> clusters, AdminTool adminTool, Clock clock,
                               Duration intervalBetweenCheckingIfOffsetsMoved, Duration offsetsMovedTimeout) {
        this.clusters = clusters;
        this.adminTool = adminTool;
        this.clock = clock;
        this.intervalBetweenCheckingIfOffsetsMoved = intervalBetweenCheckingIfOffsetsMoved;
        this.offsetsMovedTimeout = offsetsMovedTimeout;
    }

    public void manageTopic(Consumer<BrokerTopicManagement> manageFunction) {
        clusters.forEach(kafkaService -> kafkaService.manageTopic(manageFunction));
    }

    public String readMessageFromPrimary(String clusterName, Topic topic, Integer partition, Long offset) {
        return clusters.stream()
                .filter(cluster -> clusterName.equals(cluster.getClusterName()))
                .findFirst()
                .orElseThrow(() -> new BrokersClusterNotFoundException(clusterName))
                .readMessageFromPrimary(topic, partition, offset);
    }

    public MultiDCOffsetChangeSummary moveOffset(Topic topic, String subscriptionName, Long timestamp, boolean dryRun) {
        MultiDCOffsetChangeSummary multiDCOffsetChangeSummary = new MultiDCOffsetChangeSummary();

        clusters.forEach(cluster -> multiDCOffsetChangeSummary.addPartitionOffsetList(
                cluster.getClusterName(),
                cluster.indicateOffsetChange(topic, subscriptionName, timestamp, dryRun)));

        if (!dryRun) {
            logger.info("Preparing retransmission for subscription {}", topic.getQualifiedName() + "$" + subscriptionName);
            adminTool.retransmit(new SubscriptionName(subscriptionName, topic.getName()));
            clusters.forEach(clusters -> waitUntilOffsetsAreMoved(topic, subscriptionName));
        }

        return multiDCOffsetChangeSummary;
    }

    public boolean areOffsetsAvailableOnAllKafkaTopics(Topic topic) {
        return clusters.stream().allMatch(cluster -> cluster.areOffsetsAvailableOnAllKafkaTopics(topic));
    }

    public boolean topicExists(Topic topic) {
        return clusters.stream().allMatch(brokersClusterService -> brokersClusterService.topicExists(topic));
    }

    private void waitUntilOffsetsAreMoved(Topic topic, String subscriptionName) {
        Instant abortAttemptsInstant = clock.instant().plus(offsetsMovedTimeout);

        while (!areOffsetsMoved(topic, subscriptionName)) {
            if (clock.instant().isAfter(abortAttemptsInstant)) {
                logger.error("Not all offsets related to hermes subscription {}${} were moved.", topic.getQualifiedName(), subscriptionName);
                throw new UnableToMoveOffsetsException(topic, subscriptionName);
            }
            logger.debug("Not all offsets related to hermes subscription {} were moved, will retry", topic.getQualifiedName());

            sleep(intervalBetweenCheckingIfOffsetsMoved);
        }
    }

    private boolean areOffsetsMoved(Topic topic, String subscriptionName) {
        return clusters.stream()
                .allMatch(cluster -> cluster.areOffsetsMoved(topic, subscriptionName));
    }

    private void sleep(Duration sleepDuration) {
        try {
            Thread.sleep(sleepDuration.toMillis());
        } catch (InterruptedException e) {
            throw new InternalProcessingException(e);
        }
    }

    public boolean allSubscriptionsHaveConsumersAssigned(Topic topic, List<Subscription> subscriptions) {
        return clusters.stream().allMatch(brokersClusterService ->
                brokersClusterService.allSubscriptionsHaveConsumersAssigned(topic, subscriptions));
    }
}
