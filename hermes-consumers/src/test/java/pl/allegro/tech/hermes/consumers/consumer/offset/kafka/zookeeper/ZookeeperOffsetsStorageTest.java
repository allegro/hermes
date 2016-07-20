package pl.allegro.tech.hermes.consumers.consumer.offset.kafka.zookeeper;

import org.junit.After;
import org.junit.Test;
import pl.allegro.tech.hermes.common.kafka.NamespaceKafkaNamesMapper;
import pl.allegro.tech.hermes.test.helper.zookeeper.ZookeeperBaseTest;

import static org.assertj.core.api.Assertions.assertThat;
import static pl.allegro.tech.hermes.consumers.consumer.offset.SubscriptionPartition.subscriptionPartition;
import static pl.allegro.tech.hermes.consumers.consumer.offset.SubscriptionPartitionOffset.subscriptionPartitionOffset;

public class ZookeeperOffsetsStorageTest extends ZookeeperBaseTest {

    private final ZookeeperOffsetsStorage offsetsStorage = new ZookeeperOffsetsStorage(zookeeperClient, new NamespaceKafkaNamesMapper("ns"));

    @After
    public void after() throws Exception {
        deleteData("/consumers");
        deleteData("/brokers");
    }

    @Test
    public void shouldSetOffset() throws Exception {
        // given
        createOffset("kafka_topic", "kafka.topic$sub", 0, 100L);

        // when
        offsetsStorage.moveSubscriptionOffset(subscriptionPartitionOffset("kafka_topic", "kafka.topic$sub", 0, 50L));

        // then
        long offset = offsetsStorage.getSubscriptionOffset(subscriptionPartition("kafka_topic", "kafka.topic$sub", 0));
        assertThat(offset).isEqualTo(50L);
    }

    @Test
    public void shouldSetOffsetEvenIfPartitionWasNotCommittedPreviously() throws Exception {
        // given
        createOffset("kafka_topic", "kafka.topic$sub", 0, 100L);

        // when
        offsetsStorage.moveSubscriptionOffset(subscriptionPartitionOffset("kafka_topic", "kafka.topic$sub", 1, 50L));

        // then
        long offset = offsetsStorage.getSubscriptionOffset(subscriptionPartition("kafka_topic", "kafka.topic$sub", 1));
        assertThat(offset).isEqualTo(50L);
    }

    private void createOffset(String kafkaTopicName, String subscriptionName, int partitionId, Long offset) throws Exception {
        String path = offsetsStorage.getPartitionOffsetPath(subscriptionPartition(kafkaTopicName, subscriptionName, partitionId));
        zookeeperClient.create().creatingParentsIfNeeded().forPath(path);
        zookeeperClient.setData().forPath(path, offset.toString().getBytes());
    }
}
