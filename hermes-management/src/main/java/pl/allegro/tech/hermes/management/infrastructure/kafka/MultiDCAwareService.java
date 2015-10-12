package pl.allegro.tech.hermes.management.infrastructure.kafka;

import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.admin.AdminTool;
import pl.allegro.tech.hermes.management.domain.topic.BrokerTopicManagement;
import pl.allegro.tech.hermes.management.infrastructure.kafka.service.BrokersClusterService;

import java.util.List;
import java.util.function.Consumer;

public class MultiDCAwareService {

    private final List<BrokersClusterService> clusters;
    private final AdminTool adminTool;

    public MultiDCAwareService(List<BrokersClusterService> clusters, AdminTool adminTool) {
        this.clusters = clusters;
        this.adminTool = adminTool;
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
            adminTool.retransmit(new SubscriptionName(subscriptionName, topic.getName()));
        }

        return multiDCOffsetChangeSummary;
    }

    public boolean areOffsetsAvailableOnAllKafkaTopics(Topic topic) {
        return clusters.stream().allMatch(cluster -> cluster.areOffsetsAvailableOnAllKafkaTopics(topic));
    }
}
