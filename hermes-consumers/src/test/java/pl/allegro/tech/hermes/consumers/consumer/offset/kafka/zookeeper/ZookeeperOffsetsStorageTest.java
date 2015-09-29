package pl.allegro.tech.hermes.consumers.consumer.offset.kafka.zookeeper;

import org.junit.After;
import org.junit.Test;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.common.kafka.KafkaNamesMapper;
import pl.allegro.tech.hermes.common.kafka.KafkaTopic;
import pl.allegro.tech.hermes.common.kafka.offset.PartitionOffset;
import pl.allegro.tech.hermes.test.helper.zookeeper.ZookeeperBaseTest;

import static org.assertj.core.api.StrictAssertions.assertThat;

public class ZookeeperOffsetsStorageTest extends ZookeeperBaseTest {

    private static final Subscription subscription = Subscription.Builder.subscription()
            .withTopicName("brokerGroup", "brokerTopic")
            .withName("brokerSubscription").build();

    private final ZookeeperOffsetsStorage offsetsStorage = new ZookeeperOffsetsStorage(zookeeperClient, new KafkaNamesMapper("ns"));

    @After
    public void after() throws Exception {
        deleteData("/consumers");
        deleteData("/brokers");
    }

    @Test
    public void shouldSetOffset() throws Exception {
        // given
        KafkaTopic kafkaTopic = new KafkaTopic("kafka_topic");
        createOffset(subscription, kafkaTopic, 0, 100L);

        // when
        offsetsStorage.setSubscriptionOffset(subscription, new PartitionOffset(kafkaTopic, 50L, 0));

        // then
        long offset = offsetsStorage.getSubscriptionOffset(subscription, kafkaTopic, 0);
        assertThat(offset).isEqualTo(50L);
    }

    private void createOffset(Subscription subscription, KafkaTopic kafkaTopic, int partitionId, Long offset) throws Exception {
        String path = offsetsStorage.getPartitionOffsetPath(subscription, kafkaTopic, partitionId);
        zookeeperClient.create().creatingParentsIfNeeded().forPath(path);
        zookeeperClient.setData().forPath(path, offset.toString().getBytes());
    }
}
