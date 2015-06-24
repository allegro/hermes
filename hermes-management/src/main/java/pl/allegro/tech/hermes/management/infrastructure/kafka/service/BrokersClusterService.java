package pl.allegro.tech.hermes.management.infrastructure.kafka.service;

import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.management.domain.message.RetransmissionService;
import pl.allegro.tech.hermes.management.domain.topic.BrokerTopicManagement;
import pl.allegro.tech.hermes.management.domain.topic.SingleMessageReader;

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

    public String readMessage(Topic topic, Integer partition, Long offset) {
        return singleMessageReader.readMessage(topic, partition, offset);
    }

    public void indicateOffsetChange(TopicName topicName, String subscriptionName, Long timestamp) {
        retransmissionService.indicateOffsetChange(topicName, subscriptionName, clusterName, timestamp);
    }
}
