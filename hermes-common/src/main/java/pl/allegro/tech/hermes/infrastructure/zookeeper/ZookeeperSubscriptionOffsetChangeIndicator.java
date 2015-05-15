package pl.allegro.tech.hermes.infrastructure.zookeeper;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.utils.EnsurePath;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.common.exception.InternalProcessingException;
import pl.allegro.tech.hermes.domain.subscription.SubscriptionRepository;
import pl.allegro.tech.hermes.domain.subscription.offset.PartitionOffset;
import pl.allegro.tech.hermes.domain.subscription.offset.PartitionOffsets;
import pl.allegro.tech.hermes.domain.subscription.offset.SubscriptionOffsetChangeIndicator;

import static com.google.common.base.Charsets.UTF_8;

public class ZookeeperSubscriptionOffsetChangeIndicator implements SubscriptionOffsetChangeIndicator {

    private final CuratorFramework zookeeper;

    private final ZookeeperPaths paths;

    private final SubscriptionRepository subscriptionRepository;

    public ZookeeperSubscriptionOffsetChangeIndicator(
            CuratorFramework zookeeper, ZookeeperPaths paths, SubscriptionRepository repository) {

        this.zookeeper = zookeeper;
        this.paths = paths;
        this.subscriptionRepository = repository;
    }

    public void setSubscriptionOffset(
            TopicName topicName, String subscriptionName, String brokersClusterName, int partitionId, Long offset) {

        subscriptionRepository.ensureSubscriptionExists(topicName, subscriptionName);

        String offsetPath = paths.offsetPath(topicName, subscriptionName, brokersClusterName, partitionId);
        try {
            new EnsurePath(offsetPath).ensure(zookeeper.getZookeeperClient());
            zookeeper.setData().forPath(offsetPath, offset.toString().getBytes(UTF_8));
        } catch (Exception e) {
            throw new InternalProcessingException(e);
        }
    }

    public PartitionOffsets getSubscriptionOffsets(TopicName topicName, String subscriptionName, String brokersClusterName) {
        subscriptionRepository.ensureSubscriptionExists(topicName, subscriptionName);
        String offsetsPath = paths.offsetsPath(topicName, subscriptionName, brokersClusterName);

        PartitionOffsets offsets = new PartitionOffsets();
        try {
            for (String partitionAsString : zookeeper.getChildren().forPath(offsetsPath)) {
                Integer partition = Integer.valueOf(partitionAsString);
                offsets.add(new PartitionOffset(
                    getSubscriptionOffset(topicName, subscriptionName, brokersClusterName, partition),
                    partition
                ));
            }
            return offsets;
        } catch (Exception e) {
            throw new InternalProcessingException(e);
        }
    }

    private Long getSubscriptionOffset(TopicName topicName, String subscriptionName, String brokersClusterName, int partitionId)
            throws Exception {

        return Long.valueOf(new String(
            zookeeper.getData().forPath(paths.offsetPath(topicName, subscriptionName, brokersClusterName, partitionId)),
            UTF_8));
    }

}
