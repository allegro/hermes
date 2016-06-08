package pl.allegro.tech.hermes.consumers.consumer.receiver.kafka;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.test.TestingServer;
import org.junit.BeforeClass;
import org.junit.Test;
import pl.allegro.tech.hermes.api.EndpointAddress;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.common.kafka.KafkaTopicName;
import pl.allegro.tech.hermes.common.kafka.NamespaceKafkaNamesMapper;
import pl.allegro.tech.hermes.common.kafka.offset.PartitionOffset;
import pl.allegro.tech.hermes.consumers.consumer.receiver.kafka.zookeeper.ZookeeperMessageCommitter;

import java.net.MalformedURLException;
import java.nio.charset.Charset;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder.subscription;

public class ZookeeperMessageCommitterTest {

    private static final TopicName SOME_TOPIC_NAME = new TopicName("g", "b");
    private static final KafkaTopicName KAFKA_TOPIC = KafkaTopicName.valueOf("kafka_topic");

    private static CuratorFramework curatorClient;

    private ZookeeperMessageCommitter zookeeperMessageCommitter = new ZookeeperMessageCommitter(curatorClient, new NamespaceKafkaNamesMapper("ns"));

    @BeforeClass
    public static void beforeClass() throws Exception {
        TestingServer testingServer = new TestingServer();
        curatorClient = CuratorFrameworkFactory.builder()
                .connectString(testingServer.getConnectString())
                .retryPolicy(new ExponentialBackoffRetry(1000, 3))
                .build();
        curatorClient.start();
    }
//
//    @Test
//    public void shouldCommitOffsetsIfNoEntryExists() throws Exception {
//        //when
//        zookeeperMessageCommitter.commitOffset(new SubscriptionName("sub1", SOME_TOPIC_NAME), new PartitionOffset(KAFKA_TOPIC, 15, 0));
//
//        //then
//        assertEquals(16, getOffsetForPath("/consumers/ns_g_b_sub1/offsets/kafka_topic/0"));
//    }
//
//    @Test
//    public void shouldCommitOffsetsIfEntryExists() throws Exception {
//        //given
//        zookeeperMessageCommitter.commitOffset(new SubscriptionName("sub1", SOME_TOPIC_NAME), new PartitionOffset(KAFKA_TOPIC, 15, 0));
//
//        //when
//        zookeeperMessageCommitter.commitOffset(new SubscriptionName("sub1", SOME_TOPIC_NAME), new PartitionOffset(KAFKA_TOPIC, 17, 0));
//
//        //then
//        assertEquals(18, getOffsetForPath("/consumers/ns_g_b_sub1/offsets/kafka_topic/0"));
//    }
//
//    @Test
//    public void shouldCommitCorrectOffset() throws Exception {
//        //given
//        zookeeperMessageCommitter.commitOffset(new SubscriptionName("sub1", SOME_TOPIC_NAME), new PartitionOffset(KAFKA_TOPIC, 15, 0));
//
//        //when
//        zookeeperMessageCommitter.commitOffset(new SubscriptionName("sub1", SOME_TOPIC_NAME), new PartitionOffset(KAFKA_TOPIC, 17, 1));
//
//        //then
//        assertEquals(16, getOffsetForPath("/consumers/ns_g_b_sub1/offsets/kafka_topic/0"));
//        assertEquals(18, getOffsetForPath("/consumers/ns_g_b_sub1/offsets/kafka_topic/1"));
//    }
//
//    @Test
//    public void shouldRemoveOffset() throws Exception {
//        //given
//        zookeeperMessageCommitter.commitOffset(new SubscriptionName("sub1", SOME_TOPIC_NAME), new PartitionOffset(KAFKA_TOPIC, 15, 0));
//
//        //when
//        zookeeperMessageCommitter.removeOffset(SOME_TOPIC_NAME, "sub1", KAFKA_TOPIC, 0);
//
//        //then
//        assertNull(curatorClient.checkExists().forPath("/consumers/ns_g_b_sub1/offsets/kafka_topic/0"));
//    }

    private long getOffsetForPath(String path) throws Exception {
        byte[] bytes = curatorClient.getData().forPath(path);
        return Long.parseLong(new String(bytes, Charset.defaultCharset()));
    }

}
