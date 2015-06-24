package pl.allegro.tech.hermes.management.infrastructure.kafka;

import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.api.TopicName;
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

    public String readMessage(String clusterName, TopicName topicName, Integer partition, Long offset) {
        return clusters.stream()
            .filter(cluster -> clusterName.equals(cluster.getClusterName()))
            .findFirst()
            .orElseThrow(() -> new BrokersClusterNotFoundException(clusterName))
            .readMessage(topicName, partition, offset);
    }

    public MultiDCOffsetChangeSummary moveOffset(TopicName topicName, String subscriptionName, Long timestamp, boolean dryRun) {
        MultiDCOffsetChangeSummary multiDCOffsetChangeSummary = new MultiDCOffsetChangeSummary();

        clusters.forEach(cluster -> multiDCOffsetChangeSummary.addPartitionOffsetList(
                cluster.getClusterName(),
                cluster.indicateOffsetChange(topicName, subscriptionName, timestamp, dryRun)));

        if (!dryRun) {
            adminTool.retransmit(new SubscriptionName(subscriptionName, topicName));
        }

        return multiDCOffsetChangeSummary;
    }
}
