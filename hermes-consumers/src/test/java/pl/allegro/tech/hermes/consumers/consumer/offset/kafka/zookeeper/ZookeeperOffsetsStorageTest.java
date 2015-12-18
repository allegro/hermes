package pl.allegro.tech.hermes.consumers.consumer.offset.kafka.zookeeper;

import org.junit.After;
import org.junit.Test;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.common.kafka.KafkaTopicName;
import pl.allegro.tech.hermes.common.kafka.NamespaceKafkaNamesMapper;
import pl.allegro.tech.hermes.common.kafka.offset.PartitionOffset;
import pl.allegro.tech.hermes.test.helper.zookeeper.ZookeeperBaseTest;

import static org.assertj.core.api.StrictAssertions.assertThat;

public class ZookeeperOffsetsStorageTest extends ZookeeperBaseTest {

    private static final Subscription subscription = Subscription.Builder.subscription()
            .withTopicName("brokerGroup", "brokerTopic")
            .withName("brokerSubscription").build();

    private static final KafkaTopicName kafkaTopicName = KafkaTopicName.valueOf("kafka_topic");

    private final ZookeeperOffsetsStorage offsetsStorage = new ZookeeperOffsetsStorage(zookeeperClient, new NamespaceKafkaNamesMapper("ns"));

    @After
    public void after() throws Exception {
        deleteData("/consumers");
        deleteData("/brokers");
    }

    @Test
    public void shouldSetOffset() throws Exception {
        // given
        createOffset(subscription, kafkaTopicName, 0, 100L);

        // when
        offsetsStorage.setSubscriptionOffset(subscription, new PartitionOffset(kafkaTopicName, 50L, 0));

        // then
        long offset = offsetsStorage.getSubscriptionOffset(subscription, kafkaTopicName, 0);
        assertThat(offset).isEqualTo(50L);
    }

    @Test
    public void shouldSetOffsetEvenIfPartitionWasNotCommittedPreviously() throws Exception {
        // given
        createOffset(subscription, kafkaTopicName, 0, 100L);

        // when
        offsetsStorage.setSubscriptionOffset(subscription, new PartitionOffset(kafkaTopicName, 50L, 1));

        // then
        long offset = offsetsStorage.getSubscriptionOffset(subscription, kafkaTopicName, 1);
        assertThat(offset).isEqualTo(50L);
    }

    private void createOffset(Subscription subscription, KafkaTopicName kafkaTopicName, int partitionId, Long offset) throws Exception {
        String path = offsetsStorage.getPartitionOffsetPath(subscription, kafkaTopicName, partitionId);
        zookeeperClient.create().creatingParentsIfNeeded().forPath(path);
        zookeeperClient.setData().forPath(path, offset.toString().getBytes());
    }
}
