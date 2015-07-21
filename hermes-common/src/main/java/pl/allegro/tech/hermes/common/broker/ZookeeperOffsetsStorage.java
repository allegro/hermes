package pl.allegro.tech.hermes.common.broker;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import org.apache.curator.framework.CuratorFramework;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.common.exception.InternalProcessingException;

public class ZookeeperOffsetsStorage implements OffsetsStorage {

    private static final String OFFSET_PATTERN_PATH = "/consumers/%s/offsets/%s";

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
        } catch (Exception e) {
            throw new InternalProcessingException(e);
        }
    }

    private Long convertByteArrayToLong(byte[] data) {
        return Long.valueOf(new String(data, Charsets.UTF_8));
    }

    @Override
    public long getSubscriptionOffset(TopicName topicName, String subscriptionName, int partitionId) {
        try {
            byte[] offset = curatorFramework.getData().forPath(getPartitionOffsetPath(topicName, subscriptionName, partitionId));
            return Long.valueOf(new String(offset));
        } catch (Exception e) {
            throw new InternalProcessingException(e);
        }
    }

    @VisibleForTesting
    protected String getPartitionOffsetPath(TopicName topicName, String subscriptionName, int partition) {
        return Joiner.on("/").join(getOffsetPath(topicName, subscriptionName), partition);
    }

    private static String getOffsetPath(TopicName topicName, String subscriptionName) {
        return String.format(OFFSET_PATTERN_PATH, Subscription.getId(topicName, subscriptionName), topicName.qualifiedName());
    }
}
