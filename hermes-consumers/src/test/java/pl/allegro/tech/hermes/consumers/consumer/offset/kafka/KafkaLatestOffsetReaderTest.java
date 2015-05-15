package pl.allegro.tech.hermes.consumers.consumer.offset.kafka;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import kafka.common.TopicAndPartition;
import kafka.javaapi.OffsetRequest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import pl.allegro.tech.hermes.common.broker.BrokerDetails;
import pl.allegro.tech.hermes.common.broker.BrokerStorage;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.common.kafka.SimpleConsumerPool;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class KafkaLatestOffsetReaderTest {

    public static final String HOST = "host";
    public static final int PORT = 9092;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private BrokerStorage brokerStorage;

    @Mock
    private ConfigFactory configFactory;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private SimpleConsumerPool simpleConsumerPool;

    private KafkaLatestOffsetReader kafkaLatestOffsetReader;

    @Before
    public void setUp() {
        when(configFactory.getStringProperty(Configs.KAFKA_SIMPLE_CONSUMER_ID_PREFIX)).thenReturn("offsetReader");
        when(configFactory.getIntProperty(Configs.KAFKA_SIMPLE_CONSUMER_CACHE_EXPIRATION_IN_SECONDS)).thenReturn(1);
        kafkaLatestOffsetReader = new KafkaLatestOffsetReader(brokerStorage, simpleConsumerPool);
    }

    @Test
    public void shouldReadLatestOffsets() throws ExecutionException {
        int brokerId1 = 1;
        int brokerId2 = 2;
        long latestOffset1 = 40;
        long latestOffset2 = 50;
        TopicAndPartition topicAndPartition1 = new TopicAndPartition("topic1", 2);
        TopicAndPartition topicAndPartition2 = new TopicAndPartition("topic2", 4);
        Set<TopicAndPartition> topicAndPartitionSet = Sets.newHashSet(topicAndPartition1, topicAndPartition2);
        Multimap<Integer, TopicAndPartition> leadersForPartitions = ImmutableMultimap.of(
                brokerId1, topicAndPartition1,
                brokerId2, topicAndPartition2
        );
        when(brokerStorage.readLeadersForPartitions(topicAndPartitionSet)).thenReturn(leadersForPartitions);
        when(brokerStorage.readBrokerDetails(any(Integer.class))).thenReturn(new BrokerDetails(HOST, PORT));
        mockSimpleConsumer(brokerId1, topicAndPartition1, latestOffset1);
        mockSimpleConsumer(brokerId2, topicAndPartition2, latestOffset2);

        Map<TopicAndPartition, Long> latestOffsets = kafkaLatestOffsetReader.read(topicAndPartitionSet);

        assertThat(latestOffsets.get(topicAndPartition1)).isEqualTo(latestOffset1);
        assertThat(latestOffsets.get(topicAndPartition2)).isEqualTo(latestOffset2);
    }

    @Test
    public void shouldReadEmptyOffsets() {
        assertThat(kafkaLatestOffsetReader.read(Sets.newHashSet(new TopicAndPartition("topic", 2)))).isEmpty();
    }

    private void mockSimpleConsumer(int brokerId1, TopicAndPartition topicAndPartition, long latestOffset) {
        when(simpleConsumerPool
                        .get(brokerId1)
                        .getOffsetsBefore(any(OffsetRequest.class))
                        .offsets(topicAndPartition.topic(), topicAndPartition.partition())
        ).thenReturn(new long[]{latestOffset});
    }

}