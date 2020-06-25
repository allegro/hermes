package pl.allegro.tech.hermes.common.broker;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Ordering;
import com.google.common.primitives.Ints;
import kafka.cluster.Broker;
import kafka.common.TopicAndPartition;
import kafka.zk.KafkaZkClient;
import org.apache.curator.framework.CuratorFramework;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.network.ListenerName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.common.di.CuratorType;
import pl.allegro.tech.hermes.common.exception.BrokerInfoNotAvailableException;
import pl.allegro.tech.hermes.common.exception.BrokerNotFoundForPartitionException;
import pl.allegro.tech.hermes.common.exception.PartitionsNotFoundForGivenTopicException;
import scala.Option;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;
import java.util.Set;

import static com.google.common.collect.Lists.transform;

public class ZookeeperBrokerStorage implements BrokerStorage {

    private static final Logger LOGGER = LoggerFactory.getLogger(ZookeeperBrokerStorage.class);

    private static final String PARTITIONS = "/brokers/topics/%s/partitions";

    private final CuratorFramework curatorFramework;
    private final KafkaZkClient kafkaZkClient;
    private final ListenerName brokerListenerName;

    @Inject
    public ZookeeperBrokerStorage(@Named(CuratorType.KAFKA) CuratorFramework curatorFramework,
                                  KafkaZkClient kafkaZkClient, String brokerListenerName) {
        this.curatorFramework = curatorFramework;
        this.kafkaZkClient = kafkaZkClient;
        this.brokerListenerName = ListenerName.normalised(brokerListenerName);
    }

    @Override
    @SuppressWarnings("unchecked")
    public int readLeaderForPartition(TopicAndPartition topicAndPartition) {
        try {
            TopicPartition topicPartition = new TopicPartition(topicAndPartition.topic(), topicAndPartition.partition());
            return (int) kafkaZkClient.getLeaderForPartition(topicPartition).get();
        } catch (Exception exception) {
            throw new BrokerNotFoundForPartitionException(topicAndPartition.topic(), topicAndPartition.partition(), exception);
        }
    }

    @Override
    public Multimap<Integer, TopicAndPartition> readLeadersForPartitions(Set<TopicAndPartition> topicAndPartitionSet) {
        Multimap<Integer, TopicAndPartition> leadersForPartitions = ArrayListMultimap.create();
        for (TopicAndPartition topicAndPartition : topicAndPartitionSet) {
            try {
                Integer leaderId = readLeaderForPartition(topicAndPartition);
                leadersForPartitions.put(leaderId, topicAndPartition);
            } catch (BrokerNotFoundForPartitionException ex) {
                LOGGER.warn("Broker not found", ex);
            }
        }
        return leadersForPartitions;
    }

    @Override
    @SuppressWarnings("unchecked")
    public BrokerDetails readBrokerDetails(Integer brokerId) {
        try {
            Broker broker = kafkaZkClient.getBroker(brokerId).get();
            Option<Node> node = broker.getNode(brokerListenerName);
            String host = node.get().host();
            int port = node.get().port();
            return new BrokerDetails(host, port);
        } catch (Exception exception) {
            throw new BrokerInfoNotAvailableException(brokerId, exception);
        }
    }

    @Override
    public List<Integer> readPartitionsIds(String topicName) {
        try {
            List<String> partitionsAsStrings = curatorFramework.getChildren().forPath(String.format(PARTITIONS, topicName));
            List<Integer> partitions = transform(partitionsAsStrings, Ints.stringConverter());

            return Ordering.natural().sortedCopy(partitions);
        } catch (Exception exception) {
            throw new PartitionsNotFoundForGivenTopicException(topicName, exception);
        }
    }
}
