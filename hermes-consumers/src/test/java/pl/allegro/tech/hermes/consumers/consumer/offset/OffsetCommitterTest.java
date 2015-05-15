package pl.allegro.tech.hermes.consumers.consumer.offset;

import org.assertj.core.util.Lists;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.consumers.consumer.Consumer;
import pl.allegro.tech.hermes.consumers.consumer.receiver.MessageCommitter;
import pl.allegro.tech.hermes.consumers.supervisor.ConsumerHolder;
import pl.allegro.tech.hermes.domain.subscription.offset.PartitionOffset;

import static org.mockito.Matchers.anyMapOf;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class OffsetCommitterTest {

    private static final PartitionOffset PARTITION_OFFSET = new PartitionOffset(50, 4);
    private static final TopicName TOPIC_NAME_1 = new TopicName("group1", "topic");
    private static final TopicName TOPIC_NAME_2 = new TopicName("group2", "topic");
    public static final String SUBSCRIPTION_NAME = "sub";

    @Mock
    private ConfigFactory configFactory;

    @Mock
    private MessageCommitter messageCommitter;

    @Mock
    private AsyncOffsetMonitor offsetMonitor;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Consumer consumer;

    private OffsetCommitter offsetCommitter;

    private ConsumerHolder consumerHolder = new ConsumerHolder();

    @Before
    public void init() {
        when(consumer.getOffsetsToCommit()).thenReturn(Lists.newArrayList(PARTITION_OFFSET));
        offsetCommitter = new OffsetCommitter(consumerHolder, messageCommitter, configFactory, offsetMonitor);
    }

    @After
    public void clean() {
        consumerHolder.clear();
    }

    @Test
    public void shouldMonitorOffset() {
        //given
        consumerHolder.add(TOPIC_NAME_1, SUBSCRIPTION_NAME, consumer);
        consumerHolder.add(TOPIC_NAME_2, SUBSCRIPTION_NAME, consumer);

        //when
        offsetCommitter.run();

        //then
        verify(offsetMonitor).process(anyMapOf(Subscription.class, PartitionOffset.class));
    }

}