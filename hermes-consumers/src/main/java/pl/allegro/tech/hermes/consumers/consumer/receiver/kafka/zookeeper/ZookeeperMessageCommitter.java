package pl.allegro.tech.hermes.consumers.consumer.receiver.kafka.zookeeper;

import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.KeeperException;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.common.kafka.KafkaNamesMapper;
import pl.allegro.tech.hermes.consumers.consumer.receiver.MessageCommitter;
import pl.allegro.tech.hermes.domain.subscription.offset.PartitionOffset;

import java.nio.charset.Charset;

public class ZookeeperMessageCommitter implements MessageCommitter {

    private final CuratorFramework curatorFramework;
    private final KafkaNamesMapper kafkaNamesMapper;

    public ZookeeperMessageCommitter(CuratorFramework curatorFramework, KafkaNamesMapper kafkaNamesMapper) {
        this.curatorFramework = curatorFramework;
        this.kafkaNamesMapper = kafkaNamesMapper;
    }

    @Override
    public void commitOffset(Subscription subscription, PartitionOffset partitionOffset) throws Exception {
        long firstToRead = partitionOffset.getOffset() + 1;
        byte[] data = String.valueOf(firstToRead).getBytes(Charset.forName("UTF-8"));
        String offsetPath = subscriptionPath(
                subscription.getTopicName(),
                subscription.getId(),
                partitionOffset.getPartition()
        );
        try {
            curatorFramework.setData().forPath(offsetPath, data);
        } catch (KeeperException.NoNodeException ex) {
            curatorFramework.create().creatingParentsIfNeeded().forPath(offsetPath, data);
        }
    }

    private String subscriptionPath(TopicName topicName, String subscriptionId, int partition) {
        return String.format("/consumers/%s/offsets/%s/%s", subscriptionId, kafkaNamesMapper.toKafkaTopicName(topicName).asString(), partition);
    }

    @Override
    public void removeOffset(TopicName topicName, String subscriptionName, int partition) throws Exception {
        curatorFramework.delete().forPath(
            subscriptionPath(topicName, Subscription.getId(topicName, subscriptionName), partition)
        );
    }
}
