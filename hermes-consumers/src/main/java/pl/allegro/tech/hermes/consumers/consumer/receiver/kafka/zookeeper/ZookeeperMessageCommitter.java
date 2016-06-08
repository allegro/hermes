package pl.allegro.tech.hermes.consumers.consumer.receiver.kafka.zookeeper;

import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.KeeperException;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.common.kafka.KafkaNamesMapper;
import pl.allegro.tech.hermes.common.kafka.KafkaTopicName;
import pl.allegro.tech.hermes.common.kafka.KafkaZookeeperPaths;
import pl.allegro.tech.hermes.consumers.consumer.offset.SubscriptionPartitionOffset;
import pl.allegro.tech.hermes.consumers.consumer.receiver.MessageCommitter;
import pl.allegro.tech.hermes.common.kafka.offset.PartitionOffset;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Offset++, because "This is the position the consumer will pick up from if it crashes before its next commit()"
 */
public class ZookeeperMessageCommitter implements MessageCommitter {

    private final CuratorFramework curatorFramework;
    private final KafkaNamesMapper kafkaNamesMapper;

    public ZookeeperMessageCommitter(CuratorFramework curatorFramework, KafkaNamesMapper kafkaNamesMapper) {
        this.curatorFramework = curatorFramework;
        this.kafkaNamesMapper = kafkaNamesMapper;
    }

    @Override
    public void commitOffset(SubscriptionPartitionOffset partitionOffset) throws Exception {
        long firstToRead = partitionOffset.getOffset() + 1;
        byte[] data = String.valueOf(firstToRead).getBytes(StandardCharsets.UTF_8);
        String offsetPath = KafkaZookeeperPaths.partitionOffsetPath(
                kafkaNamesMapper.toConsumerGroupId(partitionOffset.getSubscriptionName()),
                partitionOffset.getKafkaTopicName(),
                partitionOffset.getPartition()
        );
        try {
            curatorFramework.setData().forPath(offsetPath, data);
        } catch (KeeperException.NoNodeException ex) {
            curatorFramework.create().creatingParentsIfNeeded().forPath(offsetPath, data);
        }
    }

    @Override
    public void removeOffset(TopicName topicName, String subscriptionName, KafkaTopicName topic, int partition) throws Exception {
        String offsetPath = KafkaZookeeperPaths.partitionOffsetPath(
                kafkaNamesMapper.toConsumerGroupId(Subscription.getId(topicName, subscriptionName)),
                topic,
                partition
        );
        curatorFramework.delete().forPath(offsetPath);
    }
}
