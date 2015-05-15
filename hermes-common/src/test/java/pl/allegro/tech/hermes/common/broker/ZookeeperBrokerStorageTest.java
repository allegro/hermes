package pl.allegro.tech.hermes.common.broker;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import kafka.common.TopicAndPartition;
import org.junit.After;
import org.junit.Test;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.common.exception.BrokerInfoNotAvailableException;
import pl.allegro.tech.hermes.common.exception.PartitionsNotFoundForGivenTopicException;
import pl.allegro.tech.hermes.test.helper.zookeeper.ZookeeperBaseTest;

import java.util.List;

import static com.googlecode.catchexception.CatchException.catchException;
import static com.googlecode.catchexception.CatchException.caughtException;
import static org.assertj.core.api.Assertions.assertThat;

public class ZookeeperBrokerStorageTest extends ZookeeperBaseTest {

    private static final TopicName TOPIC_NAME = new TopicName("brokerGroup", "brokerTopic");
    private static final String SUBSCRIPTION_NAME = "brokerSubscription";
    
    private final ZookeeperBrokerStorage brokerStorage = new ZookeeperBrokerStorage(zookeeperClient, new ObjectMapper());

    @After
    public void after() throws Exception {
        deleteData("/consumers");
        deleteData("/brokers");
    }

    @Test
    public void shouldReadLeadersForPartitions() throws Exception {
        //given
        TopicAndPartition topicAndPartition1 = new TopicAndPartition("topic1", 2);
        TopicAndPartition topicAndPartition2 = new TopicAndPartition("topic2", 4);
        createLeaderForPartition(topicAndPartition1, 0);
        createLeaderForPartition(topicAndPartition2, 3);

        //when
        Multimap<Integer, TopicAndPartition> leadersForPartitions = brokerStorage.readLeadersForPartitions(
                ImmutableSet.of(topicAndPartition1, topicAndPartition2)
        );

        //then
        assertThat(leadersForPartitions.get(0)).contains(topicAndPartition1);
        assertThat(leadersForPartitions.get(3)).contains(topicAndPartition2);
    }

    @Test
    public void shouldNotReadLeadersDueTooNoDataInZk() {
        //when
        Multimap<Integer, TopicAndPartition> leadersForPartitions = brokerStorage.readLeadersForPartitions(
            ImmutableSet.of(new TopicAndPartition("topic1", 2))
        );

        //then
        assertThat(leadersForPartitions.size()).isZero();
    }

    @Test
    public void shouldReadBrokerDetails() throws Exception {
        //given
        createBrokerDetails(1, "localhost", 9092);

        //when
        BrokerDetails details = brokerStorage.readBrokerDetails(1);

        //then
        assertThat(details.getHost()).isEqualTo("localhost");
        assertThat(details.getPort()).isEqualTo(9092);
    }

    @Test
    public void shouldGetErrorWhileReadingBrokerDetails() {
        // when
        catchException(brokerStorage).readBrokerDetails(5);
        
        // then
        assertThat((Exception) caughtException()).isInstanceOf(BrokerInfoNotAvailableException.class);
    }

    @Test
    public void shouldSetOffset() throws Exception {
        // given
        createOffset(TOPIC_NAME, SUBSCRIPTION_NAME, 0, 100L);

        // when
        brokerStorage.setSubscriptionOffset(TOPIC_NAME, SUBSCRIPTION_NAME, 0, 50L);

        // then
        assertOffset(TOPIC_NAME, SUBSCRIPTION_NAME, 0, 50L);
    }

    @Test
    public void shouldReadPartitionsIds() throws Exception {
        //given
        String topicName = "readPartitionsIdsTestTopic";
        List<Integer> partitions = Lists.newArrayList(0, 1, 2);
        createPartitionsForTopic(topicName, partitions);

        //when
        List<Integer> partitionsFromBroker = brokerStorage.readPartitionsIds(topicName);

        //then
        assertThat(partitionsFromBroker).isEqualTo(partitions);
    }

    @Test
    public void shouldThrowExceptionWhenReadPartitionsForNotExistingTopic() throws Exception {
        //given
        String nonExistingTopic = "readPartitionIdsNonExistingTopic";

        //when
        catchException(brokerStorage).readPartitionsIds(nonExistingTopic);

        //then
        assertThat((Exception) caughtException())
                .isInstanceOf(PartitionsNotFoundForGivenTopicException.class)
                .hasMessageContaining(nonExistingTopic);
    }

    private void createPartitionsForTopic(String topicName, List<Integer> partitions) throws Exception {
        for (Integer partitionId: partitions) {
            String path = String.format("/brokers/topics/%s/partitions/%s", topicName, partitionId);
            zookeeperClient.create().creatingParentsIfNeeded().forPath(path);
        }
    }

    private void createBrokerDetails(int brokerId, String host, int port) throws Exception {
        String data = String.format("{\"host\": \"%s\", \"port\": %d}", host, port);
        String path = brokerStorage.getBrokerDetailsPath(brokerId);
        zookeeperClient.create().creatingParentsIfNeeded().forPath(path);
        zookeeperClient.setData().forPath(path, data.getBytes());
    }

    private void createOffset(TopicName topicName, String subscriptionName, int partitionId, Long offset) throws Exception {
        String path = brokerStorage.getPartitionOffsetPath(topicName, subscriptionName, partitionId);
        zookeeperClient.create().creatingParentsIfNeeded().forPath(path);
        zookeeperClient.setData().forPath(path, offset.toString().getBytes());
    }

    private void createLeaderForPartition(TopicAndPartition topicAndPartition, int leaderId) throws Exception {
        String path = brokerStorage.getTopicPartitionLeaderPath(topicAndPartition);
        zookeeperClient.create().creatingParentsIfNeeded().forPath(path);
        zookeeperClient.setData().forPath(path, String.format("{\"leader\": %d}", leaderId).getBytes());
    }

    private void assertOffset(TopicName topicName, String subscriptionName, int partitionId, Long expectedOffset)
            throws Exception {

        byte [] offsetFromZk = zookeeperClient.getData().forPath(
            brokerStorage.getPartitionOffsetPath(topicName, subscriptionName, partitionId)
        );
        assertThat(Long.valueOf(new String(offsetFromZk))).isEqualTo(expectedOffset);
    }

}
