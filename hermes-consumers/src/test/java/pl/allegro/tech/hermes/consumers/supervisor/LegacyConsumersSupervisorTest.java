package pl.allegro.tech.hermes.consumers.supervisor;

import org.assertj.core.util.Lists;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import pl.allegro.tech.hermes.api.EndpointAddress;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.api.SubscriptionPolicy;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.exception.EndpointProtocolNotSupportedException;
import pl.allegro.tech.hermes.common.kafka.KafkaTopicName;
import pl.allegro.tech.hermes.common.kafka.offset.PartitionOffset;
import pl.allegro.tech.hermes.common.kafka.offset.PartitionOffsets;
import pl.allegro.tech.hermes.common.kafka.offset.SubscriptionOffsetChangeIndicator;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.consumers.consumer.SerialConsumer;
import pl.allegro.tech.hermes.consumers.consumer.offset.OffsetsStorage;
import pl.allegro.tech.hermes.consumers.consumer.receiver.MessageCommitter;
import pl.allegro.tech.hermes.consumers.message.undelivered.UndeliveredMessageLogPersister;
import pl.allegro.tech.hermes.consumers.subscription.cache.SubscriptionsCache;
import pl.allegro.tech.hermes.domain.subscription.SubscriptionRepository;
import pl.allegro.tech.hermes.domain.topic.TopicRepository;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;
import static pl.allegro.tech.hermes.api.Subscription.State.ACTIVE;
import static pl.allegro.tech.hermes.api.Subscription.State.SUSPENDED;
import static pl.allegro.tech.hermes.api.SubscriptionPolicy.Builder.subscriptionPolicy;
import static pl.allegro.tech.hermes.common.config.Configs.KAFKA_CLUSTER_NAME;
import static pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder.subscription;
import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.topic;

@RunWith(MockitoJUnitRunner.class)
public class LegacyConsumersSupervisorTest {

    private static final String SOME_SUBSCRIPTION_NAME = "sub";
    private static final TopicName SOME_TOPIC_NAME = new TopicName("group1", "topic1");
    private static final Topic SOME_TOPIC = topic(SOME_TOPIC_NAME).build();
    private static final Subscription SOME_SUBSCRIPTION = subscription(SOME_TOPIC_NAME, SOME_SUBSCRIPTION_NAME).build();

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private TopicRepository topicRepository;

    @Mock
    private SubscriptionOffsetChangeIndicator subscriptionOffsetChangeIndicator;

    @Mock
    private ConsumersExecutorService executorService;

    @Mock
    private SerialConsumer consumer;

    @Mock
    private ConsumerFactory consumerFactory;

    @Mock
    private OffsetsStorage offsetsStorage;

    @Mock
    private MessageCommitter messageCommitter;

    @Mock
    private SubscriptionsCache subscriptionsCache;

    @Mock
    private HermesMetrics hermesMetrics;

    @Mock
    private UndeliveredMessageLogPersister undeliveredMessageLogPersister;

    private ConfigFactory configFactory = new ConfigFactory();

    private LegacyConsumersSupervisor legacyConsumersSupervisor;

    private ExecutorService executor = Executors.newFixedThreadPool(50);

    @Before
    public void before() {
        when(consumerFactory.createConsumer(any(Subscription.class))).thenReturn(consumer);
        when(topicRepository.getTopicDetails(SOME_TOPIC_NAME)).thenReturn(SOME_TOPIC);

        legacyConsumersSupervisor = new LegacyConsumersSupervisor(configFactory, subscriptionRepository, topicRepository,
                subscriptionOffsetChangeIndicator, executorService, consumerFactory,
                Lists.newArrayList(messageCommitter), Lists.newArrayList(offsetsStorage), hermesMetrics,
                undeliveredMessageLogPersister);
    }

    @Test
    public void shouldRunConsumerWhenPendingSubscriptionCreated() {
        legacyConsumersSupervisor.assignConsumerForSubscription(subscription(SOME_TOPIC_NAME, "sub1").build());

        verify(executorService).execute(any(SerialConsumer.class));
    }

    @Test
    public void shouldRunConsumerWhenActiveSubscriptionCreated() {
        legacyConsumersSupervisor.assignConsumerForSubscription(subscription(SOME_TOPIC_NAME, "sub1").withState(ACTIVE).build());

        verify(executorService).execute(any(SerialConsumer.class));
    }

    @Test
    public void shouldChangeSubscriptionStateToActiveWhenCreatingConsumer() {
        Subscription subscription = subscription(SOME_TOPIC_NAME, "sub1").build();

        legacyConsumersSupervisor.assignConsumerForSubscription(subscription);

        assertThat(subscription.getState()).isEqualTo(Subscription.State.ACTIVE);
        verify(subscriptionRepository).updateSubscription(subscription);
    }

    @Test
    public void shouldNotRunConsumerWhenSuspendedSubscriptionCreated() {
        legacyConsumersSupervisor.assignConsumerForSubscription(subscription(SOME_TOPIC_NAME, "sub1").withState(SUSPENDED).build());

        verify(executorService, never()).execute(any(SerialConsumer.class));
    }

    @Test
    public void shouldShutdownConsumerWhenSubscriptionRemoved() {
        legacyConsumersSupervisor.assignConsumerForSubscription(SOME_SUBSCRIPTION);

        legacyConsumersSupervisor.deleteConsumerForSubscriptionName(SOME_SUBSCRIPTION.toSubscriptionName());

        verify(consumer).signalStop();
    }

    @Test
    public void shouldRemoveSubscriptionMetricsWhenSubscriptionRemoved() {
        legacyConsumersSupervisor.assignConsumerForSubscription(SOME_SUBSCRIPTION);
        SubscriptionName name = SOME_SUBSCRIPTION.toSubscriptionName();

        legacyConsumersSupervisor.deleteConsumerForSubscriptionName(name);

        verify(hermesMetrics).removeMetrics(name);
    }

    @Test
    public void shouldStopConsumerOnSuspend() {
        Subscription subscription = subscription(SOME_TOPIC_NAME, "sub1").build();
        legacyConsumersSupervisor.assignConsumerForSubscription(subscription);
        when(consumer.getSubscription()).thenReturn(subscription);
        Subscription modifiedSubscription = subscription(SOME_TOPIC_NAME, "sub1").withState(SUSPENDED).build();

        legacyConsumersSupervisor.notifyConsumerOnSubscriptionUpdate(modifiedSubscription);

        verify(consumer).signalStop();
    }

    @Test
    public void shouldCreateConsumerOnResume() {
        Subscription subscription = subscription(SOME_TOPIC_NAME, "sub1").withState(ACTIVE).build();

        legacyConsumersSupervisor.notifyConsumerOnSubscriptionUpdate(subscription);

        verify(consumerFactory).createConsumer(subscription);
    }

    @Test
    public void shouldReiterateConsumerWhenSubscriptionChanged() throws Exception {
        //given
        String subscriptionName = "subscriptionName1";
        String brokersClusterName = configFactory.getStringProperty(KAFKA_CLUSTER_NAME);
        PartitionOffset partitionOffset = new PartitionOffset(KafkaTopicName.valueOf("kafka_topic"), 100L, 0);
        Subscription actualSubscription = subscription(SOME_TOPIC_NAME, subscriptionName).withState(ACTIVE).build();
        SubscriptionName subscription = new SubscriptionName(subscriptionName, SOME_TOPIC_NAME);
        legacyConsumersSupervisor.notifyConsumerOnSubscriptionUpdate(actualSubscription);
        when(subscriptionOffsetChangeIndicator.getSubscriptionOffsets(SOME_TOPIC.getName(), subscriptionName, brokersClusterName))
                .thenReturn(new PartitionOffsets().add(partitionOffset));
        when(subscriptionRepository.getSubscriptionDetails(SOME_TOPIC_NAME, subscriptionName))
                .thenReturn(SOME_SUBSCRIPTION);

        when(consumer.getSubscription()).thenReturn(actualSubscription);

        //when
        legacyConsumersSupervisor.retransmit(subscription);

        //then
        verify(consumer).signalStop();
        verify(subscriptionOffsetChangeIndicator).getSubscriptionOffsets(SOME_TOPIC.getName(), subscriptionName, brokersClusterName);
        verify(offsetsStorage).setSubscriptionOffset(subscription, partitionOffset);
    }

    @Test
    public void shouldStopRegisteredConsumers() throws Exception {
        SerialConsumer firstConsumer = mock(SerialConsumer.class);
        Subscription firstSubscription = subscription(SOME_TOPIC_NAME, "sub1").build();
        SerialConsumer secondConsumer = mock(SerialConsumer.class);
        Subscription secondSubscription = subscription(SOME_TOPIC_NAME, "sub2").build();
        when(consumerFactory.createConsumer(firstSubscription)).thenReturn(firstConsumer);
        when(consumerFactory.createConsumer(secondSubscription)).thenReturn(secondConsumer);

        legacyConsumersSupervisor.start();
        legacyConsumersSupervisor.assignConsumerForSubscription(firstSubscription);
        legacyConsumersSupervisor.assignConsumerForSubscription(secondSubscription);

        legacyConsumersSupervisor.shutdown();

        verify(firstConsumer, times(1)).signalStop();
        verify(secondConsumer, times(1)).signalStop();
    }

    @Test
    public void shouldNotExecuteConsumerWhenCreatingFails() {
        when(consumerFactory.createConsumer(any(Subscription.class))).thenThrow(
                new EndpointProtocolNotSupportedException(EndpointAddress.of("xyz://localhost:8080/test"))
        );

        legacyConsumersSupervisor.assignConsumerForSubscription(SOME_SUBSCRIPTION);

        verify(executorService, never()).execute(any(SerialConsumer.class));
    }

    @Test
    public void shouldUpdateConsumerSubscription() {
        // given
        final Subscription oldSubscription = subscription(SOME_TOPIC_NAME, SOME_SUBSCRIPTION_NAME).build();
        Subscription newSubscription = subscription(SOME_TOPIC_NAME, SOME_SUBSCRIPTION_NAME).build();
        SubscriptionPolicy policy = subscriptionPolicy()
                .withRate(2)
                .withMessageTtl(1000)
                .withMessageBackoff(10)
                .build();

        newSubscription.setSerialSubscriptionPolicy(policy);

        newSubscription.setState(Subscription.State.ACTIVE);

        when(consumer.getSubscription()).thenReturn(oldSubscription);

        legacyConsumersSupervisor.assignConsumerForSubscription(oldSubscription);

        // when
        legacyConsumersSupervisor.notifyConsumerOnSubscriptionUpdate(newSubscription);

        // then
        ArgumentCaptor<Subscription> captor = ArgumentCaptor.forClass(Subscription.class);
        verify(consumerFactory).createConsumer(captor.capture());
        assertThat(captor.getValue().getSerialSubscriptionPolicy()).isEqualTo(oldSubscription.getSerialSubscriptionPolicy());

        verifyNoMoreInteractions(consumerFactory);

        verify(consumer).signalUpdate(captor.capture());
        assertThat(captor.getValue().getSerialSubscriptionPolicy()).isEqualTo(newSubscription.getSerialSubscriptionPolicy());
    }

    @Test
    public void shouldNotCreateSecondConsumerOnDuplicatedEvent() throws InterruptedException {
        // given
        final Subscription subscription = subscription(SOME_TOPIC_NAME, "sub1").build();
        final CountDownLatch latch = new CountDownLatch(100);

        // when
        for (int i = 0; i < 100; i++) {
            executor.submit(() -> {
                legacyConsumersSupervisor.assignConsumerForSubscription(subscription);
                latch.countDown();
            });
        }
        assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();

        // then
        verify(consumerFactory, times(1)).createConsumer(subscription);
    }
}
