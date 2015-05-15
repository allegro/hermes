package pl.allegro.tech.hermes.consumers.consumer.receiver.kafka;

import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.KeeperException;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.common.di.CuratorType;
import pl.allegro.tech.hermes.consumers.consumer.receiver.MessageCommitter;
import pl.allegro.tech.hermes.domain.subscription.offset.PartitionOffset;

import javax.inject.Inject;
import javax.inject.Named;
import java.nio.charset.Charset;

public class KafkaMessageCommitter implements MessageCommitter {

    private CuratorFramework curatorFramework;

    @Inject
    public KafkaMessageCommitter(@Named(CuratorType.KAFKA) CuratorFramework curatorFramework) {
        this.curatorFramework = curatorFramework;
    }

    @Override
    public void commitOffsets(Subscription subscription, PartitionOffset partitionOffset) throws Exception {
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
        return String.format("/consumers/%s/offsets/%s/%s", subscriptionId, topicName.qualifiedName(), partition);
    }

    @Override
    public void removeOffset(TopicName topicName, String subscriptionName, int partition) throws Exception {
        curatorFramework.delete().forPath(
            subscriptionPath(topicName, Subscription.getId(topicName, subscriptionName), partition)
        );
    }
}
