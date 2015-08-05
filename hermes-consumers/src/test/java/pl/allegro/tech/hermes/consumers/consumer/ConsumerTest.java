package pl.allegro.tech.hermes.consumers.consumer;

import com.google.common.util.concurrent.ListenableFuture;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import pl.allegro.tech.hermes.api.*;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.common.message.undelivered.UndeliveredMessageLog;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.consumers.consumer.converter.NoOperationMessageConverter;
import pl.allegro.tech.hermes.consumers.consumer.offset.SubscriptionOffsetCommitQueues;
import pl.allegro.tech.hermes.consumers.consumer.rate.ConsumerRateLimiter;
import pl.allegro.tech.hermes.consumers.consumer.receiver.MessageReceiver;
import pl.allegro.tech.hermes.consumers.consumer.receiver.MessageReceivingTimeoutException;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSendingResult;
import pl.allegro.tech.hermes.tracker.consumers.Trackers;

import java.util.concurrent.Semaphore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static pl.allegro.tech.hermes.api.Subscription.Builder.subscription;

@RunWith(MockitoJUnitRunner.class)
public class ConsumerTest {

    private static final Message MESSAGE = new Message(
            "id", 10, 0,
            "topic", "{\"username\":\"ala\"}".getBytes(),
            122424L,
            1224245L);

    private static final Subscription SUBSCRIPTION = Subscription.Builder.subscription()
            .withTopicName(new TopicName("group", "topic"))
            .withName("subscription")
            .withEndpoint(EndpointAddress.of("http://localhost"))
            .withSubscriptionPolicy(new SubscriptionPolicy(10, 10000, false))
            .build();

    private static final Topic TOPIC = Topic.Builder.topic().withName("group", "topic").build();

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ConfigFactory configFactory;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private MessageReceiver messageReceiver;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private HermesMetrics hermesMetrics;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Trackers trackers;

    @Mock
    private ListenableFuture<Boolean> future;

    @Mock
    private UndeliveredMessageLog undeliveredMessageLog;

    @Mock
    private ConsumerRateLimiter consumerRateLimiter;

    @Mock
    private SubscriptionOffsetCommitQueues partitionOffsetHelper;

    @Mock
    private ListenableFuture<MessageSendingResult> messageSendingResult;

    @Mock
    Semaphore infligtSemaphore;

    @Mock
    private  ConsumerMessageSender sender;

    private Consumer consumer;

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() throws Exception {
        when(configFactory.getIntProperty(Configs.REPORT_PERIOD)).thenReturn(10);
        when(configFactory.getIntProperty(Configs.CONSUMER_INFLIGHT_SIZE)).thenReturn(50);
        consumer = spy(new Consumer(messageReceiver, hermesMetrics, SUBSCRIPTION,
                consumerRateLimiter, partitionOffsetHelper, sender, infligtSemaphore, trackers, new NoOperationMessageConverter(), TOPIC));
    }

    @Test
    public void shouldReadMessagesFromDataReceiver() throws Exception {
        doReturn(true).doReturn(true).doReturn(false).when(consumer).isConsuming();

        consumer.run();

        verify(messageReceiver, times(2)).next();

        verify(messageReceiver).stop();
        verifyNoMoreInteractions(messageReceiver);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldSendUnwrappedMessageThroughMessageSender() {
        // given
        doReturn(true).doReturn(false).when(consumer).isConsuming();
        when(messageReceiver.next()).thenReturn(MESSAGE);

        // when
        consumer.run();

        // then
        verify(sender).sendMessage(MESSAGE);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldKeepReadingMessagesAfterTimeout() {
        doReturn(true).doReturn(true).doReturn(false).when(consumer).isConsuming();
        when(messageReceiver.next()).thenThrow(new MessageReceivingTimeoutException("timeout")).thenReturn(MESSAGE);

        consumer.run();

        verify(sender).sendMessage(any(Message.class));
    }

    @Test
    public void shouldIncrementInflightWhenSendingMessage() {
        //given
        when(messageReceiver.next()).thenReturn(MESSAGE);
        doReturn(true).doReturn(false).when(consumer).isConsuming();

        //when
        consumer.run();

        //then
        verify(hermesMetrics).incrementInflightCounter(SUBSCRIPTION);
    }

    @Test
    public void shouldStopConsuming() {
        consumer.stopConsuming();
        consumer.run();

        verify(messageReceiver, never()).next();
    }

    @Test
    public void shouldUpdateSubscriptionPolicy() {
        // given
        Subscription newSubscription = createSubscription();
        SubscriptionPolicy newSubscriptionPolicy = new SubscriptionPolicy(2, 500, false);
        newSubscription.setSubscriptionPolicy(newSubscriptionPolicy);

        // when
        consumer.updateSubscription(newSubscription);

        // then
        ArgumentCaptor<Subscription> captor = ArgumentCaptor.forClass(Subscription.class);

        verify(consumerRateLimiter).updateSubscription(captor.capture());
        assertThat(captor.getValue().getSubscriptionPolicy()).isEqualTo(newSubscriptionPolicy);
    }

    private Subscription createSubscription() {
        return subscription().applyDefaults()
                .withTopicName(TopicName.fromQualifiedName("group1.topic"))
                .withName("name")
                .withDescription("desc")
                .build();
    }
}
