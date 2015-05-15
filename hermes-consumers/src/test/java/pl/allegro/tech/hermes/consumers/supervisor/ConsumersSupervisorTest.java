package pl.allegro.tech.hermes.consumers.supervisor;

import com.google.common.collect.ImmutableList;
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
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.common.admin.zookeeper.ZookeeperAdminCache;
import pl.allegro.tech.hermes.common.broker.BrokerStorage;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.exception.EndpointProtocolNotSupportedException;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.consumers.consumer.Consumer;
import pl.allegro.tech.hermes.consumers.consumer.offset.AsyncOffsetMonitor;
import pl.allegro.tech.hermes.consumers.consumer.receiver.MessageCommitter;
import pl.allegro.tech.hermes.consumers.subscription.cache.SubscriptionsCache;
import pl.allegro.tech.hermes.domain.subscription.SubscriptionRepository;
import pl.allegro.tech.hermes.domain.subscription.offset.PartitionOffset;
import pl.allegro.tech.hermes.domain.subscription.offset.PartitionOffsets;
import pl.allegro.tech.hermes.domain.subscription.offset.SubscriptionOffsetChangeIndicator;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static pl.allegro.tech.hermes.api.Subscription.Builder.subscription;
import static pl.allegro.tech.hermes.api.Subscription.State.ACTIVE;
import static pl.allegro.tech.hermes.api.Subscription.State.PENDING;
import static pl.allegro.tech.hermes.api.Subscription.State.SUSPENDED;
import static pl.allegro.tech.hermes.common.config.Configs.KAFKA_CLUSTER_NAME;

@RunWith(MockitoJUnitRunner.class)
public class ConsumersSupervisorTest {

    private static final String SOME_SUBSCRIPTION_NAME = "sub";
    private static final TopicName SOME_TOPIC_NAME = new TopicName("group1", "topic1");
    private static final Subscription SOME_SUBSCRIPTION = createSubscription(SOME_TOPIC_NAME, SOME_SUBSCRIPTION_NAME);

    @Mock
    private SubscriptionRepository subscriptionRepository;
    
    @Mock
    private SubscriptionOffsetChangeIndicator subscriptionOffsetChangeIndicator;

    @Mock
    private ConsumersExecutorService executorService;

    @Mock
    private Consumer consumer;

    @Mock
    private ConsumerFactory consumerFactory;

    @Mock
    private BrokerStorage brokerStorage;

    @Mock
    private MessageCommitter messageCommitter;

    @Mock
    private AsyncOffsetMonitor asyncOffsetMonitor;

    @Mock
    private ZookeeperAdminCache adminCache;

    @Mock
    private SubscriptionsCache subscriptionsCache;

    @Mock
    private HermesMetrics hermesMetrics;

    private ConfigFactory configFactory = new ConfigFactory();

    private ConsumersSupervisor consumersSupervisor;

    private ExecutorService executor = Executors.newFixedThreadPool(50);

    @Before
    public void before() {
        when(consumerFactory.createConsumer(any(Subscription.class))).thenReturn(consumer);

        consumersSupervisor = new ConsumersSupervisor(configFactory, subscriptionRepository,
                subscriptionOffsetChangeIndicator, executorService, consumerFactory,
            messageCommitter, brokerStorage, subscriptionsCache, hermesMetrics, asyncOffsetMonitor, adminCache);
    }

    @Test
    public void shouldRegisterSubscriptionListenerOnStartup() throws Exception {
        consumersSupervisor.start();

        verify(subscriptionsCache).start(ImmutableList.of(consumersSupervisor));
    }

    @Test
    public void shouldRunConsumerWhenPendingSubscriptionCreated() {
        consumersSupervisor.onSubscriptionCreated(createSubscription(SOME_TOPIC_NAME, "sub1", PENDING));

        verify(executorService).execute(any(Consumer.class));
    }

    @Test
    public void shouldRunConsumerWhenActiveSubscriptionCreated() {
        consumersSupervisor.onSubscriptionCreated(createSubscription(SOME_TOPIC_NAME, "sub1", ACTIVE));

        verify(executorService).execute(any(Consumer.class));
    }

    @Test
    public void shouldChangeSubscriptionStateToActiveWhenCreatingConsumer() {
        Subscription subscription = createSubscription(SOME_TOPIC_NAME, "sub1", PENDING);

        consumersSupervisor.onSubscriptionCreated(subscription);

        assertThat(subscription.getState()).isEqualTo(Subscription.State.ACTIVE);
        verify(subscriptionRepository).updateSubscription(subscription);
    }

    @Test
    public void shouldNotRunConsumerWhenSuspendedSubscriptionCreated() {
        consumersSupervisor.onSubscriptionCreated(createSubscription(SOME_TOPIC_NAME, "sub1", SUSPENDED));

        verify(executorService, never()).execute(any(Consumer.class));
    }

    @Test
    public void shouldShutdownConsumerWhenSubscriptionRemoved() {
        consumersSupervisor.onSubscriptionCreated(SOME_SUBSCRIPTION);

        consumersSupervisor.onSubscriptionRemoved(SOME_SUBSCRIPTION);

        verify(consumer).stopConsuming();
    }

    @Test
    public void shouldRemoveSubscriptionMetricsWhenSubscriptionRemoved() {
        consumersSupervisor.onSubscriptionCreated(SOME_SUBSCRIPTION);

        consumersSupervisor.onSubscriptionRemoved(SOME_SUBSCRIPTION);

        verify(hermesMetrics).removeMetrics(SOME_SUBSCRIPTION);
    }

    @Test
    public void shouldStopConsumerOnSuspend() {
        Subscription subscription = createSubscription(SOME_TOPIC_NAME, "sub1");
        consumersSupervisor.onSubscriptionCreated(subscription);
        when(consumer.getSubscription()).thenReturn(subscription);
        Subscription modifiedSubscription = createSubscription(SOME_TOPIC_NAME, "sub1", SUSPENDED);

        consumersSupervisor.onSubscriptionChanged(modifiedSubscription);

        verify(consumer).stopConsuming();
    }

    @Test
    public void shouldCreateConsumerOnResume() {
        Subscription subscription = createSubscription(SOME_TOPIC_NAME, "sub1", ACTIVE);

        consumersSupervisor.onSubscriptionChanged(subscription);

        verify(consumerFactory).createConsumer(subscription);
    }

    @Test
    public void shouldReiterateConsumerWhenSubscriptionChanged() throws Exception {
        //given
        String subscriptionName = "subscriptionName1";
        String brokersClusterName = configFactory.getStringProperty(KAFKA_CLUSTER_NAME);
        Long offset = 100L;
        int partitionId = 0;
        Subscription actualSubscription = createSubscription(SOME_TOPIC_NAME, subscriptionName, ACTIVE);
        consumersSupervisor.onSubscriptionChanged(actualSubscription);
        Subscription subscription = createSubscription(SOME_TOPIC_NAME, subscriptionName);
        when(subscriptionOffsetChangeIndicator.getSubscriptionOffsets(SOME_TOPIC_NAME, subscriptionName, brokersClusterName))
                .thenReturn(new PartitionOffsets().add(new PartitionOffset(offset, partitionId)));
        when(subscriptionRepository.getSubscriptionDetails(SOME_TOPIC_NAME, subscriptionName))
                .thenReturn(SOME_SUBSCRIPTION);

        when(consumer.getSubscription()).thenReturn(actualSubscription);

        //when
        consumersSupervisor.onRetransmissionStarts(new SubscriptionName(subscription.getName(), subscription.getTopicName()));

        //then
        verify(consumer).stopConsuming();
        verify(subscriptionOffsetChangeIndicator).getSubscriptionOffsets(SOME_TOPIC_NAME, subscriptionName, brokersClusterName);
        verify(brokerStorage).setSubscriptionOffset(subscription.getTopicName(), subscription.getName(), partitionId, offset);
    }

    @Test
    public void shouldStopRegisteredConsumers() throws Exception {
        Consumer firstConsumer = mock(Consumer.class);
        Subscription firstSubscription = createSubscription(SOME_TOPIC_NAME, "sub1");
        Consumer secondConsumer = mock(Consumer.class);
        Subscription secondSubscription = createSubscription(SOME_TOPIC_NAME, "sub2");
        when(consumerFactory.createConsumer(firstSubscription)).thenReturn(firstConsumer);
        when(consumerFactory.createConsumer(secondSubscription)).thenReturn(secondConsumer);

        consumersSupervisor.start();
        consumersSupervisor.onSubscriptionCreated(firstSubscription);
        consumersSupervisor.onSubscriptionCreated(secondSubscription);

        consumersSupervisor.shutdown();

        verify(firstConsumer, times(1)).stopConsuming();
        verify(secondConsumer, times(1)).stopConsuming();
    }

    @Test
    public void shouldNotExecuteConsumerWhenCreatingFails() {
        when(consumerFactory.createConsumer(any(Subscription.class))).thenThrow(
                new EndpointProtocolNotSupportedException(EndpointAddress.of("xyz://localhost:8080/test"))
        );

        consumersSupervisor.onSubscriptionCreated(SOME_SUBSCRIPTION);

        verify(executorService, never()).execute(any(Consumer.class));
    }

    @Test
    public void shouldUpdateConsumerSubscription() {
        // given
        final Subscription oldSubscription = createSubscription(SOME_TOPIC_NAME, SOME_SUBSCRIPTION_NAME);
        Subscription newSubscription = createSubscription(SOME_TOPIC_NAME, SOME_SUBSCRIPTION_NAME);
        newSubscription.setSubscriptionPolicy(new SubscriptionPolicy(2, 1000, false));
        newSubscription.setState(Subscription.State.ACTIVE);

        when(consumer.getSubscription()).thenReturn(oldSubscription);

        consumersSupervisor.onSubscriptionCreated(oldSubscription);

        // when
        consumersSupervisor.onSubscriptionChanged(newSubscription);

        // then
        ArgumentCaptor<Subscription> captor = ArgumentCaptor.forClass(Subscription.class);
        verify(consumerFactory).createConsumer(captor.capture());
        assertThat(captor.getValue().getSubscriptionPolicy()).isEqualTo(oldSubscription.getSubscriptionPolicy());

        verifyNoMoreInteractions(consumerFactory);

        verify(consumer).updateSubscription(captor.capture());
        assertThat(captor.getValue().getSubscriptionPolicy()).isEqualTo(newSubscription.getSubscriptionPolicy());
    }

    @Test
    public void shouldNotCreateSecondConsumerOnDuplicatedEvent() throws InterruptedException {
        // given
        final Subscription subscription = createSubscription(SOME_TOPIC_NAME, "sub1", PENDING);
        final CountDownLatch latch = new CountDownLatch(100);

        // when
        for (int i = 0; i < 100; i++) {
            Future<?> submit = executor.submit(() -> {
                consumersSupervisor.onSubscriptionCreated(subscription);
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
                .withSubscriptionPolicy(new SubscriptionPolicy(1, 10000, false))
                .withDescription("desc")
                .build();
    }

}
