package pl.allegro.tech.hermes.consumers.consumer.receiver.kafka;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.test.TestingServer;
import org.junit.BeforeClass;
import org.junit.Test;
import pl.allegro.tech.hermes.api.EndpointAddress;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.common.kafka.KafkaNamesMapper;
import pl.allegro.tech.hermes.common.kafka.KafkaTopicName;
import pl.allegro.tech.hermes.consumers.consumer.receiver.kafka.zookeeper.ZookeeperMessageCommitter;
import pl.allegro.tech.hermes.common.kafka.offset.PartitionOffset;

import java.net.MalformedURLException;
import java.nio.charset.Charset;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static pl.allegro.tech.hermes.api.Subscription.Builder.subscription;
import static pl.allegro.tech.hermes.api.SubscriptionPolicy.Builder.subscriptionPolicy;

public class ZookeeperMessageCommitterTest {

    private static final TopicName SOME_TOPIC_NAME = new TopicName("g", "b");
    private static final KafkaTopicName KAFKA_TOPIC = KafkaTopicName.valueOf("kafka_topic");

    private static CuratorFramework curatorClient;

    private ZookeeperMessageCommitter zookeeperMessageCommitter = new ZookeeperMessageCommitter(curatorClient, new KafkaNamesMapper("ns"));

    @BeforeClass
    public static void beforeClass() throws Exception {
        TestingServer testingServer = new TestingServer();
        curatorClient = CuratorFrameworkFactory.builder()
                .connectString(testingServer.getConnectString())
                .retryPolicy(new ExponentialBackoffRetry(1000, 3))
                .build();
        curatorClient.start();
    }

    @Test
    public void shouldCommitOffsetsIfNoEntryExists() throws Exception {
        //when
        zookeeperMessageCommitter.commitOffset(subscriptionForTopic(SOME_TOPIC_NAME), new PartitionOffset(KAFKA_TOPIC, 15, 0));

        //then
        assertEquals(16, getOffsetForPath("/consumers/ns_g_b_sub1/offsets/ns_g.b/0"));
    }

    @Test
    public void shouldCommitOffsetsIfEntryExists() throws Exception {
        //given
        zookeeperMessageCommitter.commitOffset(subscriptionForTopic(SOME_TOPIC_NAME), new PartitionOffset(KAFKA_TOPIC, 15, 0));

        //when
        zookeeperMessageCommitter.commitOffset(subscriptionForTopic(SOME_TOPIC_NAME), new PartitionOffset(KAFKA_TOPIC, 17, 0));

        //then
        assertEquals(18, getOffsetForPath("/consumers/ns_g_b_sub1/offsets/ns_g.b/0"));
    }

    @Test
    public void shouldCommitCorrectOffset() throws Exception {
        //given
        zookeeperMessageCommitter.commitOffset(subscriptionForTopic(SOME_TOPIC_NAME), new PartitionOffset(KAFKA_TOPIC, 15, 0));

        //when
        zookeeperMessageCommitter.commitOffset(subscriptionForTopic(SOME_TOPIC_NAME), new PartitionOffset(KAFKA_TOPIC, 17, 1));

        //then
        assertEquals(16, getOffsetForPath("/consumers/ns_g_b_sub1/offsets/ns_g.b/0"));
        assertEquals(18, getOffsetForPath("/consumers/ns_g_b_sub1/offsets/ns_g.b/1"));
    }

    @Test
    public void shouldRemoveOffset() throws Exception {
        //given
        zookeeperMessageCommitter.commitOffset(subscriptionForTopic(SOME_TOPIC_NAME), new PartitionOffset(KAFKA_TOPIC, 15, 0));

        //when
        zookeeperMessageCommitter.removeOffset(SOME_TOPIC_NAME, "sub1", KAFKA_TOPIC, 0);

        //then
        assertNull(curatorClient.checkExists().forPath("/consumers/ns_g_b_sub1/offsets/ns_g.b/0"));
    }

    private Subscription subscriptionForTopic(TopicName topicName) throws MalformedURLException {
        return subscription().withTopicName(topicName).withName("sub1").withEndpoint(EndpointAddress.of("http://touk.pl"))
            .withSubscriptionPolicy(subscriptionPolicy().applyDefaults().build()).build();
    }


    private long getOffsetForPath(String path) throws Exception {
        byte[] bytes = curatorClient.getData().forPath(path);
        return Long.parseLong(new String(bytes, Charset.defaultCharset()));
    }

}
