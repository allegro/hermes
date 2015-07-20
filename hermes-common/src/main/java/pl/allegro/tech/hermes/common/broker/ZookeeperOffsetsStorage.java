package pl.allegro.tech.hermes.common.broker;

import com.google.common.base.Charsets;
import org.apache.curator.framework.CuratorFramework;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.common.exception.InternalProcessingException;

import static pl.allegro.tech.hermes.common.broker.ZookeeperOffsets.getPartitionOffsetPath;

public class ZookeeperOffsetsStorage implements OffsetsStorage {

    private final CuratorFramework curatorFramework;

    public ZookeeperOffsetsStorage(CuratorFramework curatorFramework) {
        this.curatorFramework = curatorFramework;
    }

    @Override
    public void setSubscriptionOffset(TopicName topicName, String subscriptionName, int partitionId, long offset) {
        try {
            Long actualOffset = convertByteArrayToLong(curatorFramework.getData()
                    .forPath(getPartitionOffsetPath(topicName, subscriptionName, partitionId)));

            if (actualOffset > offset) {
                curatorFramework.setData().forPath(
                        getPartitionOffsetPath(topicName, subscriptionName, partitionId),
                        Long.valueOf(offset).toString().getBytes(Charsets.UTF_8)
                );
            }
        } catch (Exception exception) {
            throw new InternalProcessingException(exception);
        }
    }

    private Long convertByteArrayToLong(byte[] data) {
        return Long.valueOf(new String(data, Charsets.UTF_8));
    }
}
