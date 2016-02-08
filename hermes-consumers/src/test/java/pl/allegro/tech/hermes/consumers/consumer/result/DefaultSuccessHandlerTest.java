package pl.allegro.tech.hermes.consumers.consumer.result;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.common.kafka.KafkaTopicName;
import pl.allegro.tech.hermes.common.kafka.offset.PartitionOffset;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.consumers.consumer.Message;
import pl.allegro.tech.hermes.consumers.consumer.offset.SubscriptionOffsetCommitQueues;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSendingResult;
import pl.allegro.tech.hermes.consumers.test.TestTrackers;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;

import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static pl.allegro.tech.hermes.consumers.test.MessageBuilder.withTestMessage;

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
    private SubscriptionOffsetCommitQueues offsetHelper;

    private Message message = withTestMessage()
            .withTopic(TOPIC_NAME)
            .withContent(MESSAGE_CONTENT, StandardCharsets.UTF_8)
            .withPartitionOffset(new PartitionOffset(KafkaTopicName.valueOf("kafka_topic"), OFFSET, PARTITION))
            .build();

    @Mock
    private Subscription subscription;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private HermesMetrics hermesMetrics;

    private TestTrackers trackers = new TestTrackers();

    private DefaultSuccessHandler defaultRetryHandler;

    private MessageSendingResult result = MessageSendingResult.succeededResult();

    @Before
    public void setUp() {
        when(subscription.getName()).thenReturn(SUBSCRIPTION_NAME);
        when(subscription.getTopicName()).thenReturn(QUALIFIED_TOPIC_NAME);
        defaultRetryHandler = new DefaultSuccessHandler(offsetHelper, hermesMetrics, trackers);
        reset(hermesMetrics);
    }

    @Test
    public void shouldDecrementOffsetWhenSuccessfullySendMessage() throws ExecutionException, InterruptedException {
        defaultRetryHandler.handle(message, subscription, result);

        verify(offsetHelper).remove(message);
    }

    @Test
    public void shouldDecrementInflightMessagesOnSuccess() {
        defaultRetryHandler.handle(message, subscription, result);

        verify(hermesMetrics).decrementInflightCounter(subscription);
    }
}
