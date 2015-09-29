package pl.allegro.tech.hermes.consumers.consumer.offset.kafka.zookeeper;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Charsets;
import org.apache.curator.framework.CuratorFramework;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.common.di.CuratorType;
import pl.allegro.tech.hermes.common.exception.InternalProcessingException;
import pl.allegro.tech.hermes.common.kafka.KafkaNamesMapper;
import pl.allegro.tech.hermes.common.kafka.KafkaTopic;
import pl.allegro.tech.hermes.common.kafka.KafkaZookeeperPaths;
import pl.allegro.tech.hermes.common.kafka.offset.PartitionOffset;
import pl.allegro.tech.hermes.consumers.consumer.offset.OffsetsStorage;

import javax.inject.Inject;
import javax.inject.Named;

public class ZookeeperOffsetsStorage implements OffsetsStorage {

    private final CuratorFramework curatorFramework;
    private final KafkaNamesMapper kafkaNamesMapper;

    @Inject
    public ZookeeperOffsetsStorage(@Named(CuratorType.KAFKA) CuratorFramework curatorFramework, KafkaNamesMapper kafkaNamesMapper) {
        this.curatorFramework = curatorFramework;
        this.kafkaNamesMapper = kafkaNamesMapper;
    }

    @Override
    public void setSubscriptionOffset(Subscription subscription, PartitionOffset partitionOffset) {
        try {
            Long actualOffset = convertByteArrayToLong(curatorFramework.getData()
                    .forPath(getPartitionOffsetPath(subscription, partitionOffset.getTopic(), partitionOffset.getPartition())));

            if (actualOffset > partitionOffset.getOffset()) {
                curatorFramework.setData().forPath(
                        getPartitionOffsetPath(subscription, partitionOffset.getTopic(), partitionOffset.getPartition()),
                        Long.valueOf(partitionOffset.getOffset()).toString().getBytes(Charsets.UTF_8)
                );
            }
        } catch (Exception e) {
            throw new InternalProcessingException(e);
        }
    }

    private Long convertByteArrayToLong(byte[] data) {
        return Long.valueOf(new String(data, Charsets.UTF_8));
    }

    @Override
    public long getSubscriptionOffset(Subscription subscription, KafkaTopic kafkaTopic, int partitionId) {
        try {
            byte[] offset = curatorFramework.getData().forPath(getPartitionOffsetPath(subscription, kafkaTopic, partitionId));
            return Long.valueOf(new String(offset));
        } catch (Exception e) {
            throw new InternalProcessingException(e);
        }
    }

    @VisibleForTesting
    protected String getPartitionOffsetPath(Subscription subscription, KafkaTopic kafkaTopic, int partition) {
        return KafkaZookeeperPaths.partitionOffsetPath(
                kafkaNamesMapper.toConsumerGroupId(subscription),
                kafkaTopic,
                partition
        );
    }

}
