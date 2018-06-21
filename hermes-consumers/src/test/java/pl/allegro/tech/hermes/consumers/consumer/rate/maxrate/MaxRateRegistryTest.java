package pl.allegro.tech.hermes.consumers.consumer.rate.maxrate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.consumers.subscription.cache.SubscriptionsCache;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;
import pl.allegro.tech.hermes.test.helper.zookeeper.ZookeeperBaseTest;

import java.util.Collections;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MaxRateRegistryTest extends ZookeeperBaseTest {

    private final ZookeeperPaths zookeeperPaths = new ZookeeperPaths("/hermes");
    private final SubscriptionName subscription = qualifiedName("subscription");
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final MaxRatePathSerializer pathSerializer = new MaxRatePathSerializer();
    private final SubscriptionsCache subscriptionsCache = mock(SubscriptionsCache.class);

    private final MaxRateRegistry maxRateRegistry = new MaxRateRegistry(
            zookeeperClient, objectMapper, zookeeperPaths, pathSerializer, subscriptionsCache);

    @Before
    public void setUp() throws Exception {
        when(subscriptionsCache.listActiveSubscriptionNames()).thenReturn(Collections.singletonList(subscription));
        maxRateRegistry.start();
    }

    @After
    public void cleanup() throws Exception {
        deleteAllNodes();
    }

    @Test
    public void shouldReturnEmptyHistoryOnInit() throws Exception {
        // when
        maxRateRegistry.ensureCorrectAssignments(subscription, Sets.newHashSet("consumer1", "consumer2"));

        // then
        assertEquals(RateHistory.empty(), maxRateRegistry.getRateHistory(consumer("consumer1")));
        assertEquals(RateHistory.empty(), maxRateRegistry.getRateHistory(consumer("consumer2")));
    }

    @Test
    public void shouldWriteAndReadRateHistoryProperly() throws Exception {
        // given
        ConsumerInstance consumer = consumer("consumer1");
        RateHistory rateHistory = RateHistory.create(0.5);

        // when
        maxRateRegistry.writeRateHistory(consumer, rateHistory);
        wait.untilZookeeperPathIsCreated(
                zookeeperPaths.consumersRateHistoryPath(consumer.getSubscription(), consumer.getConsumerId()));

        // then
        assertEquals(rateHistory, maxRateRegistry.getRateHistory(consumer));
    }

    @Test
    public void shouldWriteAndReadMaxRateProperly() throws Exception {
        // given
        ConsumerInstance consumer1 = consumer("consumer1");
        ConsumerInstance consumer2 = consumer("consumer2");

        // when
        maxRateRegistry.update(subscription,
                ImmutableMap.of(
                        "consumer1", new MaxRate(350.0),
                        "consumer2", new MaxRate(0.5)
                ));

        wait.untilZookeeperPathIsCreated(
                zookeeperPaths.consumersMaxRatePath(consumer1.getSubscription(), consumer1.getConsumerId()));
        wait.untilZookeeperPathIsCreated(
                zookeeperPaths.consumersMaxRatePath(consumer2.getSubscription(), consumer2.getConsumerId()));

        // then
        assertEquals(new MaxRate(350.0), maxRateRegistry.getMaxRate(consumer1).get());
        assertEquals(new MaxRate(0.5), maxRateRegistry.getMaxRate(consumer2).get());
    }

    @Test
    public void shouldRemoveInactiveConsumerEntries() throws Exception {
        // given
        ConsumerInstance consumer1 = consumer("consumer1");
        ConsumerInstance consumer2 = consumer("consumer2");
        maxRateRegistry.ensureCorrectAssignments(subscription, Sets.newHashSet("consumer1", "consumer2"));
        maxRateRegistry.update(subscription, ImmutableMap.of(
                "consumer1", new MaxRate(350.0),
                "consumer2", new MaxRate(0.5)
        ));

        wait.untilZookeeperPathIsCreated(
                zookeeperPaths.consumersMaxRatePath(consumer1.getSubscription(), consumer1.getConsumerId()));
        wait.untilZookeeperPathIsCreated(
                zookeeperPaths.consumersMaxRatePath(consumer2.getSubscription(), consumer2.getConsumerId()));

        // when
        maxRateRegistry.ensureCorrectAssignments(subscription, Sets.newHashSet("consumer1", "consumer3"));
        wait.untilZookeeperPathNotExists(
                zookeeperPaths.consumersRatePath(consumer2.getSubscription(), consumer2.getConsumerId()));

        // then
        assertEquals(Optional.empty(), maxRateRegistry.getMaxRate(consumer2));
    }

    @Test
    public void shouldProvideConsumerWithSensibleDefaults() throws Exception {
        // when
        RateHistory rateHistory = maxRateRegistry.getRateHistory(consumer("consumer1"));
        Optional<MaxRate> maxRate = maxRateRegistry.getMaxRate(consumer("consumer1"));

        // then
        assertEquals(RateHistory.empty(), rateHistory);
        assertEquals(Optional.empty(), maxRate);
    }

    private ConsumerInstance consumer(String consumerId) {
        return new ConsumerInstance(consumerId, subscription);
    }

    private static SubscriptionName qualifiedName(String name) {
        return SubscriptionName.fromString("com.test.topic$" + name);
    }
}
