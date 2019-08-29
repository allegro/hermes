package pl.allegro.tech.hermes.consumers.consumer.rate.maxrate;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.consumers.subscription.cache.SubscriptionsCache;
import pl.allegro.tech.hermes.consumers.subscription.id.SubscriptionId;
import pl.allegro.tech.hermes.consumers.subscription.id.SubscriptionIds;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;
import pl.allegro.tech.hermes.test.helper.config.MutableConfigFactory;
import pl.allegro.tech.hermes.test.helper.zookeeper.ZookeeperBaseTest;

import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static com.jayway.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FlatBinaryMaxRateRegistryTest extends ZookeeperBaseTest {

    private final SubscriptionName subscription1 = SubscriptionName.fromString("pl.allegro.tech.hermes$testSubscription");
    private final SubscriptionId subscriptionId1 = SubscriptionId.from(subscription1, 1L);
    private final SubscriptionName subscription2 = SubscriptionName.fromString("pl.allegro.tech.hermes$testSubscription2");
    private final SubscriptionId subscriptionId2 = SubscriptionId.from(subscription2, 2L);

    private final SubscriptionsCache subscriptionsCache = mock(SubscriptionsCache.class);
    private final SubscriptionIds subscriptionIds = mock(SubscriptionIds.class);

    private final ZookeeperPaths zookeeperPaths = new ZookeeperPaths("/hermes");
    private final ConfigFactory configFactory = new MutableConfigFactory();

    private final FlatBinaryMaxRateRegistry.AssignedConsumersSupplier assignedConsumersSupplier = mock(FlatBinaryMaxRateRegistry.AssignedConsumersSupplier.class);
    private final FlatBinaryMaxRateRegistry.AssignedSubscriptionsSupplier assignedSubscriptionsSupplier = mock(FlatBinaryMaxRateRegistry.AssignedSubscriptionsSupplier.class);

    private final FlatBinaryMaxRateRegistry maxRateRegistry = new FlatBinaryMaxRateRegistry(configFactory,
            assignedConsumersSupplier, assignedSubscriptionsSupplier, zookeeperClient, zookeeperPaths, subscriptionIds);

    private final String consumerId = configFactory.getStringProperty(Configs.CONSUMER_WORKLOAD_NODE_ID);
    private final String cluster = configFactory.getStringProperty(Configs.KAFKA_CLUSTER_NAME);
    private final FlatBinaryMaxRateRegistryPaths paths = new FlatBinaryMaxRateRegistryPaths(zookeeperPaths, consumerId, cluster);

    @Before
    public void setUp() {
        when(subscriptionsCache.listActiveSubscriptionNames()).thenReturn(ImmutableList.of(subscription1, subscription2));

        when(subscriptionIds.getSubscriptionId(subscription1)).thenReturn(Optional.of(subscriptionId1));
        when(subscriptionIds.getSubscriptionId(subscriptionId1.getValue())).thenReturn(Optional.of(subscriptionId1));
        when(subscriptionIds.getSubscriptionId(subscription2)).thenReturn(Optional.of(subscriptionId2));
        when(subscriptionIds.getSubscriptionId(subscriptionId2.getValue())).thenReturn(Optional.of(subscriptionId2));

        when(assignedConsumersSupplier.getAllAssignedConsumers()).thenReturn(ImmutableSet.of(consumerId));
        when(assignedSubscriptionsSupplier.getAssignedSubscriptions(consumerId)).thenReturn(ImmutableSet.of(subscription1, subscription2));
        maxRateRegistry.start();
    }

    @After
    public void cleanup() {
        try {
            deleteAllNodes();
        } catch (Exception e) {
            e.printStackTrace();
        }
        maxRateRegistry.stop();
    }

    @Test
    public void shouldHaveEmptyRateHistoryOnInit() {
        // given
        maxRateRegistry.onBeforeMaxRateCalculation(); // read

        // and
        maxRateRegistry.ensureCorrectAssignments(subscription1, ImmutableSet.of(consumerId));

        // then
        assertEquals(RateHistory.empty(), maxRateRegistry.getRateHistory(new ConsumerInstance(consumerId, subscription1)));
    }

    @Test
    public void shouldWriteAndReadRateHistoryForCurrentConsumer() {
        // given
        ConsumerInstance consumer = new ConsumerInstance(consumerId, subscription1);

        RateHistory rateHistory = RateHistory.create(0.5);

        // when
        maxRateRegistry.writeRateHistory(consumer, rateHistory);

        // and
        maxRateRegistry.onAfterWriteRateHistories(); // store

        // then
        wait.untilZookeeperPathIsCreated(paths.currentConsumerRateHistoryPath());

        // when
        maxRateRegistry.onBeforeMaxRateCalculation(); // read

        // and
        RateHistory readHistory = maxRateRegistry.getRateHistory(consumer);

        // then
        assertEquals(rateHistory, readHistory);
    }

    @Test
    public void shouldWriteAndReadMaxRateForCurrentConsumer() {
        // when
        maxRateRegistry.update(subscription1, ImmutableMap.of(consumerId, new MaxRate(0.5)));
        maxRateRegistry.update(subscription2, ImmutableMap.of(consumerId, new MaxRate(350.0)));

        // and
        maxRateRegistry.onAfterMaxRateCalculation(); // store

        // then
        wait.untilZookeeperPathIsCreated(paths.consumerMaxRatePath(consumerId));

        // when
        maxRateRegistry.onBeforeMaxRateCalculation(); // read

        // then
        assertEquals(new MaxRate(0.5), maxRateRegistry.getMaxRate(new ConsumerInstance(consumerId, subscription1)).get());
        assertEquals(new MaxRate(350.0), maxRateRegistry.getMaxRate(new ConsumerInstance(consumerId, subscription2)).get());
    }

    @Test
    public void shouldCleanRegistryFromInactiveConsumerNodes() {
        // when
        maxRateRegistry.update(subscription1, ImmutableMap.of(consumerId, new MaxRate(350.0)));

        // and
        maxRateRegistry.onAfterMaxRateCalculation(); // store

        // then
        wait.untilZookeeperPathIsCreated(paths.consumerMaxRatePath(consumerId));

        // when
        when(assignedConsumersSupplier.getAllAssignedConsumers()).thenReturn(Collections.emptySet());
        maxRateRegistry.onBeforeMaxRateCalculation(); // cleanup

        // then
        wait.untilZookeeperPathNotExists(paths.consumerMaxRatePath(consumerId));
    }

    @Test
    public void shouldCleanupConsumerMaxRateFromPreviouslyAssignedSubscriptions() {
        // when
        maxRateRegistry.update(subscription1, ImmutableMap.of(consumerId, new MaxRate(350.0)));
        maxRateRegistry.update(subscription2, ImmutableMap.of(consumerId, new MaxRate(5.0)));

        // and
        maxRateRegistry.onAfterMaxRateCalculation(); // store

        // then
        wait.untilZookeeperPathIsCreated(paths.consumerMaxRatePath(consumerId));

        // when
        when(assignedSubscriptionsSupplier.getAssignedSubscriptions(consumerId))
                .thenReturn(Collections.singleton(subscription1));

        // and
        maxRateRegistry.onBeforeMaxRateCalculation(); // read and cleanup
        maxRateRegistry.onAfterMaxRateCalculation(); // store

        // then
        await().atMost(2, TimeUnit.SECONDS)
                .until((() -> maxRateRegistry.getMaxRate(new ConsumerInstance(consumerId, subscription1)).isPresent()
                        && !maxRateRegistry.getMaxRate(new ConsumerInstance(consumerId, subscription2)).isPresent()));
    }

    @Test(expected = IllegalStateException.class)
    public void shouldNotAllowForSavingRateHistoryOfOtherThanCurrentConsumerNode() {
        // when
        maxRateRegistry.writeRateHistory(new ConsumerInstance("otherConsumer", subscription1), RateHistory.create(0.5));
    }

    @Test(expected = IllegalStateException.class)
    public void shouldNotAllowForReadingRateHistoryOfOtherThanCurrentConsumerNode() {
        // when
        maxRateRegistry.getRateHistory(new ConsumerInstance("otherConsumer", subscription1));
    }

    @Test(expected = IllegalStateException.class)
    public void shouldNotAllowForReadingMaxRateOfOtherThanCurrentConsumerNodeConsumerRateHistoriesDecoder() {
        // when
        maxRateRegistry.getMaxRate(new ConsumerInstance("otherConsumer", subscription1));
    }
}
