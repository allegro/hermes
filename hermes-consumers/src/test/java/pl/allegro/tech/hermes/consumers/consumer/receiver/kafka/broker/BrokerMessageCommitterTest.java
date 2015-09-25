package pl.allegro.tech.hermes.consumers.consumer.receiver.kafka.broker;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.consumers.consumer.offset.kafka.broker.BrokerOffsetsRepository;
import pl.allegro.tech.hermes.common.kafka.offset.PartitionOffset;

import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class BrokerMessageCommitterTest {

    @Mock
    private BrokerOffsetsRepository brokerOffsetsRepository;


    @Test
    public void shouldCommitOffset() throws Exception {
        //given
        int offset = 0;
        int partition = 0;
        Subscription subscription = Subscription.Builder.subscription().withTopicName("group", "topic").withName("sub").build();

        BrokerMessageCommitter brokerMessageCommitter = new BrokerMessageCommitter(brokerOffsetsRepository);

        //when
        brokerMessageCommitter.commitOffset(subscription, new PartitionOffset(offset, partition));

        //then
        verify(brokerOffsetsRepository).save(subscription, new PartitionOffset(offset + 1, partition));
    }
}