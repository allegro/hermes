package pl.allegro.tech.hermes.infrastructure.zookeeper;

import org.apache.curator.framework.CuratorFramework;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.common.exception.InternalProcessingException;
import pl.allegro.tech.hermes.common.kafka.KafkaTopic;
import pl.allegro.tech.hermes.common.kafka.KafkaTopicName;
import pl.allegro.tech.hermes.common.kafka.offset.PartitionOffset;
import pl.allegro.tech.hermes.common.kafka.offset.PartitionOffsets;
import pl.allegro.tech.hermes.common.kafka.offset.SubscriptionOffsetChangeIndicator;
import pl.allegro.tech.hermes.domain.subscription.SubscriptionRepository;

import java.nio.charset.StandardCharsets;
import java.util.List;

public class ZookeeperSubscriptionOffsetChangeIndicator implements SubscriptionOffsetChangeIndicator {

    private static final Logger logger = LoggerFactory.getLogger(ZookeeperSubscriptionOffsetChangeIndicator.class);

    private final CuratorFramework zookeeper;

    private final ZookeeperPaths paths;

    private final SubscriptionRepository subscriptionRepository;

    public ZookeeperSubscriptionOffsetChangeIndicator(
            CuratorFramework zookeeper, ZookeeperPaths paths, SubscriptionRepository repository) {

        this.zookeeper = zookeeper;
        this.paths = paths;
        this.subscriptionRepository = repository;
    }

    @Override
    public void setSubscriptionOffset(TopicName topicName, String subscriptionName, String brokersClusterName, PartitionOffset partitionOffset) {
        subscriptionRepository.ensureSubscriptionExists(topicName, subscriptionName);

        String offsetPath = paths.offsetPath(topicName, subscriptionName, partitionOffset.getTopic(), brokersClusterName, partitionOffset.getPartition());
        try {
            byte[] offset = String.valueOf(partitionOffset.getOffset()).getBytes(StandardCharsets.UTF_8);
            if (zookeeper.checkExists().forPath(offsetPath) == null) {
                zookeeper.create()
                        .creatingParentsIfNeeded()
                        .forPath(offsetPath, offset);
            } else {
                zookeeper.setData().forPath(offsetPath, offset);
            }
        } catch (Exception e) {
            throw new InternalProcessingException(e);
        }
    }

    @Override
    public PartitionOffsets getSubscriptionOffsets(TopicName topic, String subscriptionName, String brokersClusterName) {
        subscriptionRepository.ensureSubscriptionExists(topic, subscriptionName);
        String kafkaTopicsPath = paths.subscribedKafkaTopicsPath(topic, subscriptionName);

        PartitionOffsets allOffsets = new PartitionOffsets();
        getZookeeperChildrenForPath(kafkaTopicsPath).stream().map(KafkaTopicName::valueOf).forEach(kafkaTopic ->
                allOffsets.addAll(getOffsetsForKafkaTopic(topic, kafkaTopic, subscriptionName, brokersClusterName))
        );
        return allOffsets;
    }

    @Override
    public boolean areOffsetsMoved(TopicName topicName, String subscriptionName, String brokersClusterName,
                                   KafkaTopic kafkaTopic, List<Integer> partitionIds) {
        return partitionIds.stream().allMatch(partitionId -> offsetDoesNotExist(topicName, subscriptionName, brokersClusterName, partitionId, kafkaTopic));
    }

    @Override
    public void removeOffset(TopicName topicName, String subscriptionName, String brokersClusterName, KafkaTopicName kafkaTopicName, int partitionId) {
        String offsetPath = paths.offsetPath(topicName, subscriptionName, kafkaTopicName, brokersClusterName, partitionId);

        try {
            zookeeper.delete().guaranteed().deletingChildrenIfNeeded().forPath(offsetPath);
        } catch (Exception e) {
            throw new InternalProcessingException(e);
        }
    }

    private boolean offsetDoesNotExist(TopicName topicName, String subscriptionName, String brokersClusterName, Integer partitionId,  KafkaTopic kafkaTopic) {
        String offsetPath = paths.offsetPath(topicName, subscriptionName, kafkaTopic.name(), brokersClusterName, partitionId);
        try {
            boolean result = zookeeper.checkExists().forPath(offsetPath) == null;
            if (!result) {
                logger.info("Leftover on path {}", offsetPath);
            }
            return result;
        } catch (Exception e) {
            throw new InternalProcessingException(e);
        }
    }

    private PartitionOffsets getOffsetsForKafkaTopic(TopicName topic, KafkaTopicName kafkaTopicName, String subscriptionName, String brokersClusterName) {
        String offsetsPath = paths.offsetsPath(topic, subscriptionName, kafkaTopicName, brokersClusterName);

        PartitionOffsets offsets = new PartitionOffsets();
        for (String partitionAsString : getZookeeperChildrenForPath(offsetsPath)) {
            Integer partition = Integer.valueOf(partitionAsString);
            offsets.add(new PartitionOffset(
                    kafkaTopicName,
                    getOffsetForPartition(topic, kafkaTopicName, subscriptionName, brokersClusterName, partition),
                    partition
            ));
        }
        return offsets;
    }

    private List<String> getZookeeperChildrenForPath(String path) {
        try {
            return zookeeper.getChildren().forPath(path);
        } catch (Exception e) {
            throw new InternalProcessingException(e);
        }
    }

    private Long getOffsetForPartition(TopicName topic, KafkaTopicName kafkaTopicName,
                                       String subscriptionName, String brokersClusterName, int partitionId) {
        try {
            String offsetPath = paths.offsetPath(topic,
                    subscriptionName,
                    kafkaTopicName,
                    brokersClusterName,
                    partitionId);
            return Long.valueOf(new String(zookeeper.getData().forPath(offsetPath), StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new InternalProcessingException(e);
        }
    }

}
