package pl.allegro.tech.hermes.consumers.consumer.result;

import com.codahale.metrics.Counter;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.common.exception.InternalProcessingException;
import pl.allegro.tech.hermes.common.kafka.KafkaTopicName;
import pl.allegro.tech.hermes.common.kafka.offset.PartitionOffset;
import pl.allegro.tech.hermes.common.message.undelivered.UndeliveredMessageLog;
import pl.allegro.tech.hermes.common.metric.Counters;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.consumers.consumer.Message;
import pl.allegro.tech.hermes.consumers.consumer.offset.SubscriptionOffsetCommitQueues;
import pl.allegro.tech.hermes.consumers.test.TestTrackers;

import java.nio.charset.StandardCharsets;
import java.time.Clock;

import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static pl.allegro.tech.hermes.api.SentMessageTrace.createUndeliveredMessage;
import static pl.allegro.tech.hermes.consumers.consumer.sender.MessageSendingResult.failedResult;
import static pl.allegro.tech.hermes.consumers.test.MessageBuilder.withTestMessage;

@RunWith(MockitoJUnitRunner.class)
public class DefaultErrorHandlerTest {

    private static final long OFFSET = 124412L;
    private static final int PARTITION = 5;
    private static final String TOPIC_NAME = "topic0";
    private static final String GROUP_NAME = "group0";
    private static final TopicName QUALIFIED_TOPIC_NAME = new TopicName(GROUP_NAME, TOPIC_NAME);
    private static final String SUBSCRIPTION_NAME = "subscription0";
    private static final String MESSAGE_CONTENT = "test";
    private static final Long CURRENT_TIME = 8L;
    private static final String CLUSTER = "cluster";

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private HermesMetrics hermesMetrics;

    @Mock
    private SubscriptionOffsetCommitQueues offsetHelper;

    @Mock
    private UndeliveredMessageLog undeliveredMessageLog;

    @Mock
    private Subscription subscription;

    private final Message message = withTestMessage()
            .withTopic(TOPIC_NAME)
            .withContent(MESSAGE_CONTENT, StandardCharsets.UTF_8)
            .withPartitionOffset(new PartitionOffset(KafkaTopicName.valueOf("kafka_topic"),
                    OFFSET, PARTITION))
            .build();

    @Mock
    private Clock clock;

    @Mock
    private Counter counter;

    private TestTrackers trackers = new TestTrackers();

    private DefaultErrorHandler defaultErrorHandler;


    @Before
    public void setUp() {
        when(subscription.getName()).thenReturn(SUBSCRIPTION_NAME);
        when(subscription.getTopicName()).thenReturn(QUALIFIED_TOPIC_NAME);
        when(clock.millis()).thenReturn(CURRENT_TIME);
        defaultErrorHandler = new DefaultErrorHandler(offsetHelper, hermesMetrics, undeliveredMessageLog, clock, trackers, CLUSTER);
        reset(hermesMetrics);
    }

    @Test
    public void shouldDecrementOffsetWhenExhaustedRetries() {
        defaultErrorHandler.handleDiscarded(message, subscription, failedResult(new InternalProcessingException("oops")));

        verify(offsetHelper).remove(message);
    }

    @Test
    public void shouldDecrementInflightMessagesWhenPolicyExhausted() {
        //when
        defaultErrorHandler.handleDiscarded(message, subscription, failedResult(new InternalProcessingException("cause")));

        //then
        verify(hermesMetrics, times(1)).decrementInflightCounter(subscription);
    }

    @Test
    public void shouldAddDiscardedEventToUndeliveredMessageLogWhenPolicyExhausted() {
        //given
        when(hermesMetrics.counter(Counters.INFLIGHT, QUALIFIED_TOPIC_NAME, SUBSCRIPTION_NAME)).thenReturn(counter);
        InternalProcessingException cause = new InternalProcessingException("Test cause.");

        //when
        defaultErrorHandler.handleDiscarded(message, subscription, failedResult(cause));

        //then
        verify(undeliveredMessageLog)
                .add(createUndeliveredMessage(subscription, MESSAGE_CONTENT, cause, CURRENT_TIME, PARTITION, OFFSET, CLUSTER));
    }
}
