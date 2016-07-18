package pl.allegro.tech.hermes.consumers.consumer.offset.kafka.zookeeper;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Charsets;
import org.apache.curator.framework.CuratorFramework;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.common.di.CuratorType;
import pl.allegro.tech.hermes.common.exception.InternalProcessingException;
import pl.allegro.tech.hermes.common.kafka.KafkaNamesMapper;
import pl.allegro.tech.hermes.common.kafka.KafkaTopicName;
import pl.allegro.tech.hermes.common.kafka.KafkaZookeeperPaths;
import pl.allegro.tech.hermes.common.kafka.offset.PartitionOffset;
import pl.allegro.tech.hermes.consumers.consumer.offset.OffsetsStorage;
import pl.allegro.tech.hermes.consumers.consumer.offset.SubscriptionPartition;
import pl.allegro.tech.hermes.consumers.consumer.offset.SubscriptionPartitionOffset;

import javax.inject.Inject;
import javax.inject.Named;

public class ZookeeperOffsetsStorage implements OffsetsStorage {

    public static final int OFFSET_MISSING = -1;

    private final CuratorFramework curatorFramework;
    private final KafkaNamesMapper kafkaNamesMapper;

    @Inject
    public ZookeeperOffsetsStorage(@Named(CuratorType.KAFKA) CuratorFramework curatorFramework, KafkaNamesMapper kafkaNamesMapper) {
        this.curatorFramework = curatorFramework;
        this.kafkaNamesMapper = kafkaNamesMapper;
    }

    @Override
    public void moveSubscriptionOffset(SubscriptionPartitionOffset partitionOffset) {
        try {
            String offsetPath = getPartitionOffsetPath(partitionOffset.getSubscriptionPartition());

            long currentOffset = OFFSET_MISSING;
            if (curatorFramework.checkExists().forPath(offsetPath) != null) {
                currentOffset = convertByteArrayToLong(curatorFramework.getData().forPath(offsetPath));
            }

            if (currentOffset == OFFSET_MISSING || currentOffset > partitionOffset.getOffset()) {
                if (currentOffset == OFFSET_MISSING) {
                    curatorFramework.create().creatingParentsIfNeeded().forPath(offsetPath);
                }

                curatorFramework.setData().forPath(
                        offsetPath,
                        Long.valueOf(partitionOffset.getOffset()).toString().getBytes(Charsets.UTF_8)
                );
            }
        } catch (Exception e) {
            throw new InternalProcessingException(e);
        }
    }

    private long convertByteArrayToLong(byte[] data) {
        return Long.valueOf(new String(data, Charsets.UTF_8));
    }

    @Override
    public long getSubscriptionOffset(SubscriptionPartition subscriptionPartition) {
        try {
            byte[] offset = curatorFramework.getData().forPath(getPartitionOffsetPath(subscriptionPartition));
            return Long.valueOf(new String(offset));
        } catch (Exception e) {
            throw new InternalProcessingException(e);
        }
    }

    @VisibleForTesting
    protected String getPartitionOffsetPath(SubscriptionPartition partitionOffset) {
        return KafkaZookeeperPaths.partitionOffsetPath(
                kafkaNamesMapper.toConsumerGroupId(partitionOffset.getSubscriptionName()),
                partitionOffset.getKafkaTopicName(),
                partitionOffset.getPartition()
        );
    }

}
