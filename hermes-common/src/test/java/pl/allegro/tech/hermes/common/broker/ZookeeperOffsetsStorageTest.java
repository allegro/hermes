package pl.allegro.tech.hermes.common.broker;

import org.junit.After;
import org.junit.Test;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.test.helper.zookeeper.ZookeeperBaseTest;

import static org.assertj.core.api.StrictAssertions.assertThat;
import static pl.allegro.tech.hermes.common.broker.ZookeeperOffsets.getPartitionOffsetPath;

public class ZookeeperOffsetsStorageTest extends ZookeeperBaseTest {

    private static final TopicName TOPIC_NAME = new TopicName("brokerGroup", "brokerTopic");
    private static final String SUBSCRIPTION_NAME = "brokerSubscription";

    private final OffsetsStorage offsetsStorage = new ZookeeperOffsetsStorage(zookeeperClient);

    @After
    public void after() throws Exception {
        deleteData("/consumers");
        deleteData("/brokers");
    }

    @Test
    public void shouldSetOffset() throws Exception {
        // given
        createOffset(TOPIC_NAME, SUBSCRIPTION_NAME, 0, 100L);

        // when
        offsetsStorage.setSubscriptionOffset(TOPIC_NAME, SUBSCRIPTION_NAME, 0, 50L);

        // then
        assertOffset(TOPIC_NAME, SUBSCRIPTION_NAME, 0, 50L);
    }

    private void createOffset(TopicName topicName, String subscriptionName, int partitionId, Long offset) throws Exception {
        String path = getPartitionOffsetPath(topicName, subscriptionName, partitionId);
        zookeeperClient.create().creatingParentsIfNeeded().forPath(path);
        zookeeperClient.setData().forPath(path, offset.toString().getBytes());
    }

    private void assertOffset(TopicName topicName, String subscriptionName, int partitionId, Long expectedOffset)
            throws Exception {

        byte [] offsetFromZk = zookeeperClient.getData().forPath(
                getPartitionOffsetPath(topicName, subscriptionName, partitionId)
        );
        assertThat(Long.valueOf(new String(offsetFromZk))).isEqualTo(expectedOffset);
    }
}
