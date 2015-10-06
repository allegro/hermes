package pl.allegro.tech.hermes.management.infrastructure.kafka.service;

import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.kafka.KafkaNamesMapper;
import pl.allegro.tech.hermes.common.kafka.offset.PartitionOffset;
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
    private final KafkaNamesMapper kafkaNamesMapper;

    public BrokersClusterService(String clusterName, SingleMessageReader singleMessageReader,
                                 RetransmissionService retransmissionService, BrokerTopicManagement brokerTopicManagement,
                                 KafkaNamesMapper kafkaNamesMapper) {

        this.clusterName = clusterName;
        this.singleMessageReader = singleMessageReader;
        this.retransmissionService = retransmissionService;
        this.brokerTopicManagement = brokerTopicManagement;
        this.kafkaNamesMapper = kafkaNamesMapper;
    }

    public String getClusterName() {
        return clusterName;
    }

    public void manageTopic(Consumer<BrokerTopicManagement> manageFunction) {
        manageFunction.accept(brokerTopicManagement);
    }

    public String readMessageFromPrimary(Topic topic, Integer partition, Long offset) {
        return singleMessageReader.readMessageAsJson(topic, kafkaNamesMapper.toKafkaTopics(topic).getPrimary(), partition, offset);
    }

    public List<PartitionOffset> indicateOffsetChange(Topic topic, String subscriptionName, Long timestamp, boolean dryRun) {
        return retransmissionService.indicateOffsetChange(topic, subscriptionName, clusterName, timestamp, dryRun);
    }
}
