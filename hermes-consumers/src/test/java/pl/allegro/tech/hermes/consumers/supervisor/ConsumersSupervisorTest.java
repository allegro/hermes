package pl.allegro.tech.hermes.consumers.supervisor;

import org.assertj.core.util.Lists;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import pl.allegro.tech.hermes.api.*;
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
import static pl.allegro.tech.hermes.api.Subscription.Builder.subscription;
import static pl.allegro.tech.hermes.api.Subscription.State.ACTIVE;
import static pl.allegro.tech.hermes.api.Subscription.State.PENDING;
import static pl.allegro.tech.hermes.api.Subscription.State.SUSPENDED;
import static pl.allegro.tech.hermes.api.SubscriptionPolicy.Builder.subscriptionPolicy;
import static pl.allegro.tech.hermes.api.Topic.Builder.topic;
import static pl.allegro.tech.hermes.common.config.Configs.KAFKA_CLUSTER_NAME;

@RunWith(MockitoJUnitRunner.class)
public class ConsumersSupervisorTest {

    private static final String SOME_SUBSCRIPTION_NAME = "sub";
    private static final TopicName SOME_TOPIC_NAME = new TopicName("group1", "topic1");
    private static final Topic SOME_TOPIC = topic().applyDefaults().withName(SOME_TOPIC_NAME).build();
    private static final Subscription SOME_SUBSCRIPTION = createSubscription(SOME_TOPIC_NAME, SOME_SUBSCRIPTION_NAME);

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

    private ConsumersSupervisor consumersSupervisor;

    private ExecutorService executor = Executors.newFixedThreadPool(50);

    @Before
    public void before() {
        when(consumerFactory.createConsumer(any(Subscription.class))).thenReturn(consumer);
        when(topicRepository.getTopicDetails(SOME_TOPIC_NAME)).thenReturn(SOME_TOPIC);

        consumersSupervisor = new ConsumersSupervisor(configFactory, subscriptionRepository, topicRepository,
                subscriptionOffsetChangeIndicator, executorService, consumerFactory,
                Lists.newArrayList(messageCommitter), Lists.newArrayList(offsetsStorage), hermesMetrics,
                undeliveredMessageLogPersister);
    }

    @Test
    public void shouldRunConsumerWhenPendingSubscriptionCreated() {
        consumersSupervisor.assignConsumerForSubscription(createSubscription(SOME_TOPIC_NAME, "sub1", PENDING));

        verify(executorService).execute(any(SerialConsumer.class));
    }

    @Test
    public void shouldRunConsumerWhenActiveSubscriptionCreated() {
        consumersSupervisor.assignConsumerForSubscription(createSubscription(SOME_TOPIC_NAME, "sub1", ACTIVE));

        verify(executorService).execute(any(SerialConsumer.class));
    }

    @Test
    public void shouldChangeSubscriptionStateToActiveWhenCreatingConsumer() {
        Subscription subscription = createSubscription(SOME_TOPIC_NAME, "sub1", PENDING);

        consumersSupervisor.assignConsumerForSubscription(subscription);

        assertThat(subscription.getState()).isEqualTo(Subscription.State.ACTIVE);
        verify(subscriptionRepository).updateSubscription(subscription);
    }

    @Test
    public void shouldNotRunConsumerWhenSuspendedSubscriptionCreated() {
        consumersSupervisor.assignConsumerForSubscription(createSubscription(SOME_TOPIC_NAME, "sub1", SUSPENDED));

        verify(executorService, never()).execute(any(SerialConsumer.class));
    }

    @Test
    public void shouldShutdownConsumerWhenSubscriptionRemoved() {
        consumersSupervisor.assignConsumerForSubscription(SOME_SUBSCRIPTION);

        consumersSupervisor.deleteConsumerForSubscriptionName(SOME_SUBSCRIPTION.toSubscriptionName());

        verify(consumer).stopConsuming();
    }

    @Test
    public void shouldRemoveSubscriptionMetricsWhenSubscriptionRemoved() {
        consumersSupervisor.assignConsumerForSubscription(SOME_SUBSCRIPTION);
        SubscriptionName name = SOME_SUBSCRIPTION.toSubscriptionName();

        consumersSupervisor.deleteConsumerForSubscriptionName(name);

        verify(hermesMetrics).removeMetrics(name);
    }

    @Test
    public void shouldStopConsumerOnSuspend() {
        Subscription subscription = createSubscription(SOME_TOPIC_NAME, "sub1");
        consumersSupervisor.assignConsumerForSubscription(subscription);
        when(consumer.getSubscription()).thenReturn(subscription);
        Subscription modifiedSubscription = createSubscription(SOME_TOPIC_NAME, "sub1", SUSPENDED);

        consumersSupervisor.notifyConsumerOnSubscriptionUpdate(modifiedSubscription);

        verify(consumer).stopConsuming();
    }

    @Test
    public void shouldCreateConsumerOnResume() {
        Subscription subscription = createSubscription(SOME_TOPIC_NAME, "sub1", ACTIVE);

        consumersSupervisor.notifyConsumerOnSubscriptionUpdate(subscription);

        verify(consumerFactory).createConsumer(subscription);
    }

    @Test
    public void shouldReiterateConsumerWhenSubscriptionChanged() throws Exception {
        //given
        String subscriptionName = "subscriptionName1";
        String brokersClusterName = configFactory.getStringProperty(KAFKA_CLUSTER_NAME);
        PartitionOffset partitionOffset = new PartitionOffset(KafkaTopicName.valueOf("kafka_topic"), 100L, 0);
        Subscription actualSubscription = createSubscription(SOME_TOPIC_NAME, subscriptionName, ACTIVE);
        SubscriptionName subscription = new SubscriptionName(subscriptionName, SOME_TOPIC_NAME);
        consumersSupervisor.notifyConsumerOnSubscriptionUpdate(actualSubscription);
        when(subscriptionOffsetChangeIndicator.getSubscriptionOffsets(SOME_TOPIC, subscriptionName, brokersClusterName))
                .thenReturn(new PartitionOffsets().add(partitionOffset));
        when(subscriptionRepository.getSubscriptionDetails(SOME_TOPIC_NAME, subscriptionName))
                .thenReturn(SOME_SUBSCRIPTION);

        when(consumer.getSubscription()).thenReturn(actualSubscription);

        //when
        consumersSupervisor.retransmit(subscription);

        //then
        verify(consumer).stopConsuming();
        verify(subscriptionOffsetChangeIndicator).getSubscriptionOffsets(SOME_TOPIC, subscriptionName, brokersClusterName);
        verify(offsetsStorage).setSubscriptionOffset(Subscription.fromSubscriptionName(subscription), partitionOffset);
    }

    @Test
    public void shouldStopRegisteredConsumers() throws Exception {
        SerialConsumer firstConsumer = mock(SerialConsumer.class);
        Subscription firstSubscription = createSubscription(SOME_TOPIC_NAME, "sub1");
        SerialConsumer secondConsumer = mock(SerialConsumer.class);
        Subscription secondSubscription = createSubscription(SOME_TOPIC_NAME, "sub2");
        when(consumerFactory.createConsumer(firstSubscription)).thenReturn(firstConsumer);
        when(consumerFactory.createConsumer(secondSubscription)).thenReturn(secondConsumer);

        consumersSupervisor.start();
        consumersSupervisor.assignConsumerForSubscription(firstSubscription);
        consumersSupervisor.assignConsumerForSubscription(secondSubscription);

        consumersSupervisor.shutdown();

        verify(firstConsumer, times(1)).stopConsuming();
        verify(secondConsumer, times(1)).stopConsuming();
    }

    @Test
    public void shouldNotExecuteConsumerWhenCreatingFails() {
        when(consumerFactory.createConsumer(any(Subscription.class))).thenThrow(
                new EndpointProtocolNotSupportedException(EndpointAddress.of("xyz://localhost:8080/test"))
        );

        consumersSupervisor.assignConsumerForSubscription(SOME_SUBSCRIPTION);

        verify(executorService, never()).execute(any(SerialConsumer.class));
    }

    @Test
    public void shouldUpdateConsumerSubscription() {
        // given
        final Subscription oldSubscription = createSubscription(SOME_TOPIC_NAME, SOME_SUBSCRIPTION_NAME);
        Subscription newSubscription = createSubscription(SOME_TOPIC_NAME, SOME_SUBSCRIPTION_NAME);
        SubscriptionPolicy policy = subscriptionPolicy()
                .withRate(2)
                .withMessageTtl(1000)
                .withMessageBackoff(10)
                .build();

        newSubscription.setSerialSubscriptionPolicy(policy);

        newSubscription.setState(Subscription.State.ACTIVE);

        when(consumer.getSubscription()).thenReturn(oldSubscription);

        consumersSupervisor.assignConsumerForSubscription(oldSubscription);

        // when
        consumersSupervisor.notifyConsumerOnSubscriptionUpdate(newSubscription);

        // then
        ArgumentCaptor<Subscription> captor = ArgumentCaptor.forClass(Subscription.class);
        verify(consumerFactory).createConsumer(captor.capture());
        assertThat(captor.getValue().getSerialSubscriptionPolicy()).isEqualTo(oldSubscription.getSerialSubscriptionPolicy());

        verifyNoMoreInteractions(consumerFactory);

        verify(consumer).updateSubscription(captor.capture());
        assertThat(captor.getValue().getSerialSubscriptionPolicy()).isEqualTo(newSubscription.getSerialSubscriptionPolicy());
    }

    @Test
    public void shouldNotCreateSecondConsumerOnDuplicatedEvent() throws InterruptedException {
        // given
        final Subscription subscription = createSubscription(SOME_TOPIC_NAME, "sub1", PENDING);
        final CountDownLatch latch = new CountDownLatch(100);

        // when
        for (int i = 0; i < 100; i++) {
            executor.submit(() -> {
                consumersSupervisor.assignConsumerForSubscription(subscription);
                latch.countDown();
            });
        }
        assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();

        // then
        verify(consumerFactory, times(1)).createConsumer(subscription);
    }

    private static Subscription createSubscription(TopicName topicName, String subscriptionName, Subscription.State state) {
        Subscription subscription = createSubscription(topicName, subscriptionName);
        subscription.setState(state);
        return subscription;
    }

    private static Subscription createSubscription(TopicName topicName, String subscriptionName) {
        return subscription().applyDefaults()
                .withTopicName(topicName)
                .withName(subscriptionName)
                .withSubscriptionPolicy(subscriptionPolicy().applyDefaults().build())
                .withDescription("desc")
                .build();
    }

}
