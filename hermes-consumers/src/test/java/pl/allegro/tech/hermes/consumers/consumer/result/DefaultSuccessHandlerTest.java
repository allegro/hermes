package pl.allegro.tech.hermes.consumers.consumer.result;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.consumers.consumer.offset.PartitionOffsetHelper;
import pl.allegro.tech.hermes.consumers.consumer.receiver.Message;
import pl.allegro.tech.hermes.consumers.test.TestTrackers;

import java.util.Optional;
import java.util.concurrent.ExecutionException;

import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DefaultSuccessHandlerTest {

    private static final long OFFSET = 124412L;
    private static final int PARTITION = 5;
    private static final String TOPIC_NAME = "topic0";
    private static final String GROUP_NAME = "group0";
    private static final TopicName QUALIFIED_TOPIC_NAME = new TopicName(GROUP_NAME, TOPIC_NAME);
    private static final String SUBSCRIPTION_NAME = "subscription0";
    private static final String MESSAGE_CONTENT = "test";

    @Mock
    private PartitionOffsetHelper offsetHelper;

    private Message message = new Message(
            Optional.of("id"), OFFSET, PARTITION, TOPIC_NAME, MESSAGE_CONTENT.getBytes(), Optional.of(241243123L), Optional.of(2412431234L)
    );

    @Mock
    private Subscription subscription;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private HermesMetrics hermesMetrics;

    private TestTrackers trackers;

    private DefaultSuccessHandler defaultRetryHandler;

    @Before
    public void setUp() {
        when(subscription.getName()).thenReturn(SUBSCRIPTION_NAME);
        when(subscription.getTopicName()).thenReturn(QUALIFIED_TOPIC_NAME);
        trackers = new TestTrackers();
        defaultRetryHandler = new DefaultSuccessHandler(offsetHelper, hermesMetrics, trackers);
        reset(hermesMetrics);
    }

    @Test
    public void shouldDecrementOffsetWhenSuccessfullySendMessage() throws ExecutionException, InterruptedException {
        defaultRetryHandler.handle(message, subscription);

        verify(offsetHelper).decrement(PARTITION, OFFSET);
    }

    @Test
    public void shouldDecrementInflightMessagesOnSuccess() {
        defaultRetryHandler.handle(message, subscription);

        verify(hermesMetrics).decrementInflightCounter(subscription);
    }
}
