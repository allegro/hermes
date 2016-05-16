package pl.allegro.tech.hermes.consumers.consumer;

import com.google.common.util.concurrent.ListenableFuture;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.SubscriptionPolicy;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.common.message.undelivered.UndeliveredMessageLog;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.consumers.consumer.converter.MessageConverterResolver;
import pl.allegro.tech.hermes.consumers.consumer.converter.NoOperationMessageConverter;
import pl.allegro.tech.hermes.consumers.consumer.offset.SubscriptionOffsetCommitQueues;
import pl.allegro.tech.hermes.consumers.consumer.rate.ConsumerRateLimiter;
import pl.allegro.tech.hermes.consumers.consumer.rate.InflightsPool;
import pl.allegro.tech.hermes.consumers.consumer.receiver.MessageReceiver;
import pl.allegro.tech.hermes.consumers.consumer.receiver.MessageReceivingTimeoutException;
import pl.allegro.tech.hermes.consumers.consumer.receiver.ReceiverFactory;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSendingResult;
import pl.allegro.tech.hermes.tracker.consumers.Trackers;

import java.nio.charset.StandardCharsets;
import java.time.Clock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static pl.allegro.tech.hermes.api.SubscriptionPolicy.Builder.subscriptionPolicy;
import static pl.allegro.tech.hermes.consumers.test.MessageBuilder.withTestMessage;
import static pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder.subscription;
import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.topic;


@RunWith(MockitoJUnitRunner.class)
public class ConsumerTest {

    private static final Message MESSAGE = withTestMessage()
            .withContent("{\"username\":\"ala\"}", StandardCharsets.UTF_8)
            .build();

    private static final Subscription SUBSCRIPTION = subscription("group.topic", "subscription").build();

    private static final Topic TOPIC = topic("group", "topic").build();

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ConfigFactory configFactory;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ReceiverFactory messageReceiverFactory;

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
    private MessageConverterResolver messageConverterResolver;

    @Mock
    private InflightsPool releasable;

    @Mock
    private ConsumerMessageSender sender;

    @Mock
    private ConsumerMessageSenderFactory consumerMessageSenderFactory;

    private SerialConsumer consumer;

//    @Before
//    @SuppressWarnings("unchecked")
//    public void setUp() throws Exception {
//        when(configFactory.getIntProperty(Configs.REPORT_PERIOD)).thenReturn(10);
//        when(configFactory.getIntProperty(Configs.CONSUMER_INFLIGHT_SIZE)).thenReturn(50);
//        when(messageReceiverFactory.createMessageReceiver(any(Topic.class), any(Subscription.class))).thenReturn(messageReceiver);
//        when(messageConverterResolver.converterFor(any(Message.class), any(Subscription.class)))
//                .thenReturn(new NoOperationMessageConverter());
//        when(consumerMessageSenderFactory.create(any(Subscription.class), any(ConsumerRateLimiter.class),
//                any(SubscriptionOffsetCommitQueues.class), any(InflightsPool.class))).thenReturn(sender);
//
//        consumer = spy(new SerialConsumer(messageReceiverFactory, hermesMetrics, SUBSCRIPTION,
//                consumerRateLimiter, partitionOffsetHelper, consumerMessageSenderFactory, trackers, messageConverterResolver, TOPIC, Clock.systemUTC(), configFactory));
//
//        doNothing().when(consumer).setThreadName();
//        doNothing().when(consumer).unsetThreadName();
//    }
//
//    @Test
//    public void shouldReadMessagesFromDataReceiver() throws Exception {
//        doReturn(true).doReturn(true).doReturn(false).when(consumer).isConsuming();
//
//        consumer.run();
//
//        verify(messageReceiver, times(2)).next();
//
//        verify(messageReceiver).stop();
//        verifyNoMoreInteractions(messageReceiver);
//    }
//
//    @Test
//    @SuppressWarnings("unchecked")
//    public void shouldSendUnwrappedMessageThroughMessageSender() {
//        // given
//        doReturn(true).doReturn(false).when(consumer).isConsuming();
//        when(messageReceiver.next()).thenReturn(MESSAGE);
//
//        // when
//        consumer.run();
//
//        // then
//        verify(sender).sendMessage(MESSAGE);
//    }
//
//    @Test
//    @SuppressWarnings("unchecked")
//    public void shouldKeepReadingMessagesAfterTimeout() {
//        doReturn(true).doReturn(true).doReturn(false).when(consumer).isConsuming();
//        when(messageReceiver.next()).thenThrow(new MessageReceivingTimeoutException("timeout")).thenReturn(MESSAGE);
//
//        consumer.run();
//
//        verify(sender).sendMessage(any(Message.class));
//    }
//
//    @Test
//    public void shouldIncrementInflightWhenSendingMessage() {
//        //given
//        when(messageReceiver.next()).thenReturn(MESSAGE);
//        doReturn(true).doReturn(false).when(consumer).isConsuming();
//
//        //when
//        consumer.run();
//
//        //then
//        verify(hermesMetrics).incrementInflightCounter(SUBSCRIPTION);
//    }
//
//    @Test
//    public void shouldStopConsuming() {
//        consumer.stopConsuming();
//        consumer.run();
//
//        verify(messageReceiver, never()).next();
//    }
//
//    @Test
//    public void shouldUpdateSubscriptionPolicy() {
//        // given
//        Subscription newSubscription = subscription("group.topic", "subscription").build();
//        SubscriptionPolicy newSubscriptionPolicy = subscriptionPolicy()
//                .withRate(2)
//                .withMessageTtl(500)
//                .withMessageBackoff(10)
//                .build();
//        newSubscription.setSerialSubscriptionPolicy(newSubscriptionPolicy);
//
//        // when
//        consumer.updateSubscription(newSubscription);
//
//        // then
//        ArgumentCaptor<Subscription> captor = ArgumentCaptor.forClass(Subscription.class);
//
//        verify(consumerRateLimiter).updateSubscription(captor.capture());
//        assertThat(captor.getValue().getSerialSubscriptionPolicy()).isEqualTo(newSubscriptionPolicy);
//    }
}
