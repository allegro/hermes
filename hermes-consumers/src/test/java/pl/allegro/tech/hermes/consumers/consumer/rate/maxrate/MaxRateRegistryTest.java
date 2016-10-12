package pl.allegro.tech.hermes.consumers.consumer.rate.maxrate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import org.junit.After;
import org.junit.Test;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;
import pl.allegro.tech.hermes.test.helper.zookeeper.ZookeeperBaseTest;

import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder.subscription;

public class MaxRateRegistryTest extends ZookeeperBaseTest {

    private final ZookeeperPaths zookeeperPaths = new ZookeeperPaths("/hermes");
    private final Subscription subscription = createSubscription("subscription");
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final MaxRateRegistry maxRateRegistry = new MaxRateRegistry(
            zookeeperClient, objectMapper, zookeeperPaths);

    @After
    public void cleanup() throws Exception {
        deleteAllNodes();
    }

    @Test
    public void shouldCreatePathsInZookeeperOnInit() throws Exception {
        // when
        maxRateRegistry.ensureCorrectAssignments(subscription, Sets.newHashSet("consumer1", "consumer2"));

        // then
        assertEquals(RateHistory.empty(), maxRateRegistry.readOrCreateRateHistory(subscription, "consumer1"));
        assertEquals(RateHistory.empty(), maxRateRegistry.readOrCreateRateHistory(subscription, "consumer2"));
    }

    @Test
    public void shouldWriteAndReadRateHistoryProperly() throws Exception {
        // when
        RateHistory rateHistory = RateHistory.create(0.5);
        maxRateRegistry.writeRateHistory(subscription, "consumer1", rateHistory);

        // then
        assertEquals(rateHistory, maxRateRegistry.readOrCreateRateHistory(subscription, "consumer1"));
    }

    @Test
    public void shouldWriteAndReadMaxRateProperly() throws Exception {
        // when
        maxRateRegistry.update(subscription,
                ImmutableMap.of(
                        "consumer1", new MaxRate(350.0),
                        "consumer2", new MaxRate(0.5)
                ));

        // then
        assertEquals(new MaxRate(350.0), maxRateRegistry.readMaxRate(subscription, "consumer1").get());
        assertEquals(new MaxRate(0.5), maxRateRegistry.readMaxRate(subscription, "consumer2").get());
    }

    @Test
    public void shouldRemoveInactiveConsumerEntries() throws Exception {
        // given
        maxRateRegistry.ensureCorrectAssignments(subscription, Sets.newHashSet("consumer1", "consumer2"));

        // when
        maxRateRegistry.ensureCorrectAssignments(subscription, Sets.newHashSet("consumer1", "consumer3"));

        // then
        assertEquals(Optional.empty(), maxRateRegistry.readMaxRate(subscription, "consumer2"));
        assertEquals(RateHistory.empty(), maxRateRegistry.readOrCreateRateHistory(subscription, "consumer1"));
        assertEquals(RateHistory.empty(), maxRateRegistry.readOrCreateRateHistory(subscription, "consumer3"));
    }

    @Test
    public void shouldProvideConsumerWithSensibleDefaults() throws Exception {
        // when
        RateHistory rateHistory = maxRateRegistry.readOrCreateRateHistory(subscription, "consumer1");
        Optional<MaxRate> maxRate = maxRateRegistry.readMaxRate(subscription, "consumer1");

        // then
        assertEquals(RateHistory.empty(), rateHistory);
        assertEquals(Optional.empty(), maxRate);
    }

    private static Subscription createSubscription(String name) {
        SubscriptionName subscriptionName = SubscriptionName.fromString("com.test.topic$" + name);
        Subscription subscription = subscription(subscriptionName).build();
        return subscription;
    }
}
