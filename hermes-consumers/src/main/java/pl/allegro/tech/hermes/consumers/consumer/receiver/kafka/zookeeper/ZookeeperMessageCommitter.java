package pl.allegro.tech.hermes.consumers.consumer.receiver.kafka.zookeeper;

import com.codahale.metrics.Timer;
import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.KeeperException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.common.kafka.KafkaNamesMapper;
import pl.allegro.tech.hermes.common.kafka.KafkaTopicName;
import pl.allegro.tech.hermes.common.kafka.KafkaZookeeperPaths;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.consumers.consumer.offset.FailedToCommitOffsets;
import pl.allegro.tech.hermes.consumers.consumer.offset.OffsetsToCommit;
import pl.allegro.tech.hermes.consumers.consumer.offset.SubscriptionPartitionOffset;
import pl.allegro.tech.hermes.consumers.consumer.receiver.MessageCommitter;

import java.nio.charset.StandardCharsets;

public class ZookeeperMessageCommitter implements MessageCommitter {

    private static final Logger logger = LoggerFactory.getLogger(ZookeeperMessageCommitter.class);

    private final CuratorFramework curatorFramework;
    private final KafkaNamesMapper kafkaNamesMapper;
    private final HermesMetrics metrics;

    public ZookeeperMessageCommitter(CuratorFramework curatorFramework,
                                     KafkaNamesMapper kafkaNamesMapper,
                                     HermesMetrics metrics) {
        this.curatorFramework = curatorFramework;
        this.kafkaNamesMapper = kafkaNamesMapper;
        this.metrics = metrics;
    }

    @Override
    public FailedToCommitOffsets commitOffsets(OffsetsToCommit offsetsToCommit) {
        FailedToCommitOffsets failedOffsets = new FailedToCommitOffsets();
        for (SubscriptionName subscriptionName : offsetsToCommit.subscriptionNames()) {
            for (SubscriptionPartitionOffset offset : offsetsToCommit.batchFor(subscriptionName)) {
                try(Timer.Context c = metrics.timer("offset-committer.single-commit.zookeeper").time()) {
                    commitOffset(offset);
                } catch (Exception exception) {
                    logger.warn("Failed to commit offset {}", offset);
                    failedOffsets.add(offset);
                }
            }
        }
        return failedOffsets;
    }

    private void commitOffset(SubscriptionPartitionOffset partitionOffset) throws Exception {
        byte[] data = String.valueOf(partitionOffset.getOffset()).getBytes(StandardCharsets.UTF_8);
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
                kafkaNamesMapper.toConsumerGroupId(new SubscriptionName(subscriptionName, topicName)),
                topic,
                partition
        );
        curatorFramework.delete().forPath(offsetPath);
    }
}
