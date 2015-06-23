package pl.allegro.tech.hermes.management.infrastructure.kafka.service;

import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.domain.subscription.offset.PartitionOffset;
import pl.allegro.tech.hermes.management.domain.message.RetransmissionService;
import pl.allegro.tech.hermes.management.domain.topic.BrokerTopicManagement;
import pl.allegro.tech.hermes.management.domain.topic.SingleMessageReader;

import java.util.List;
import java.util.function.Consumer;

public class BrokersClusterService {

    private final String clusterName;
    private final SingleMessageReader singleMessageReader;
    private final RetransmissionService retransmissionService;
    private final BrokerTopicManagement brokerTopicManagement;

    public BrokersClusterService(String clusterName, SingleMessageReader singleMessageReader,
                                 RetransmissionService retransmissionService, BrokerTopicManagement brokerTopicManagement) {

        this.clusterName = clusterName;
        this.singleMessageReader = singleMessageReader;
        this.retransmissionService = retransmissionService;
        this.brokerTopicManagement = brokerTopicManagement;
    }

    public String getClusterName() {
        return clusterName;
    }

    public void manageTopic(Consumer<BrokerTopicManagement> manageFunction) {
        manageFunction.accept(brokerTopicManagement);
    }

    public String readMessage(TopicName topicName, Integer partition, Long offset) {
        return singleMessageReader.readMessage(topicName, partition, offset);
    }

    public List<PartitionOffset> indicateOffsetChange(TopicName topicName, String subscriptionName, Long timestamp, boolean dryRun) {
        return retransmissionService.indicateOffsetChange(topicName, subscriptionName, clusterName, timestamp, dryRun);
    }
}
