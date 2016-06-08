package pl.allegro.tech.hermes.consumers.consumer.receiver.kafka.broker;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.common.kafka.KafkaTopicName;
import pl.allegro.tech.hermes.common.kafka.offset.PartitionOffset;
import pl.allegro.tech.hermes.consumers.consumer.offset.kafka.broker.BrokerOffsetsRepository;

import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class BrokerMessageCommitterTest {

    private static final KafkaTopicName KAFKA_TOPIC = KafkaTopicName.valueOf("kafka_topic");

    @Mock
    private BrokerOffsetsRepository brokerOffsetsRepository;

    @Test
    public void shouldCommitOffset() throws Exception {
        //given
        int offset = 0;
        int partition = 0;
//        SubscriptionName subscriptionName =  new SubscriptionName("sub", TopicName.fromQualifiedName("group.topic"));
//
//        BrokerMessageCommitter brokerMessageCommitter = new BrokerMessageCommitter(brokerOffsetsRepository);
//
//        //when
//        brokerMessageCommitter.commitOffset(subscriptionName, new PartitionOffset(KAFKA_TOPIC, offset, partition));
//
//        //then
//        verify(brokerOffsetsRepository).save(subscriptionName, new PartitionOffset(KAFKA_TOPIC, offset + 1, partition));
    }
}