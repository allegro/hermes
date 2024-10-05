package pl.allegro.tech.hermes.consumers.supervisor.workload.weighted;

import static java.time.temporal.ChronoUnit.MILLIS;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.consumers.subscription.id.SubscriptionId;
import pl.allegro.tech.hermes.consumers.subscription.id.SubscriptionIds;
import pl.allegro.tech.hermes.consumers.supervisor.workload.TestSubscriptionIds;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;
import pl.allegro.tech.hermes.test.helper.zookeeper.ZookeeperBaseTest;

public class ZookeeperSubscriptionProfileRegistryTest extends ZookeeperBaseTest {

  @Test
  public void shouldPersistAndReadSubscriptionProfiles() {
    // given
    SubscriptionName firstSubscription =
        SubscriptionName.fromString("pl.allegro.tech.hermes$testSubscription");
    SubscriptionName secondSubscription =
        SubscriptionName.fromString("pl.allegro.tech.hermes$testSubscription2");
    SubscriptionIds subscriptionIds =
        new TestSubscriptionIds(
            List.of(
                SubscriptionId.from(firstSubscription, -1422951212L),
                SubscriptionId.from(secondSubscription, 2L)));
    ZookeeperSubscriptionProfileRegistry registry =
        new ZookeeperSubscriptionProfileRegistry(
            zookeeperClient,
            subscriptionIds,
            new ZookeeperPaths("/hermes"),
            "kafka-cluster",
            100_000);
    SubscriptionProfiles profiles =
        new SubscriptionProfiles(
            Map.of(
                firstSubscription,
                new SubscriptionProfile(Instant.now(), new Weight(100d)),
                secondSubscription,
                SubscriptionProfile.UNDEFINED),
            Instant.now());

    // when
    registry.persist(profiles);

    // then
    SubscriptionProfiles readProfiles = registry.fetch();
    assertThatProfilesAreEqual(readProfiles, profiles);
  }

  @Test
  public void shouldPersistAndReadEmptySubscriptionProfiles() {
    // given
    ZookeeperSubscriptionProfileRegistry registry =
        new ZookeeperSubscriptionProfileRegistry(
            zookeeperClient,
            new TestSubscriptionIds(List.of()),
            new ZookeeperPaths("/hermes"),
            "kafka-cluster",
            100_000);
    SubscriptionProfiles profiles = SubscriptionProfiles.EMPTY;

    // when
    registry.persist(profiles);

    // then
    SubscriptionProfiles readProfiles = registry.fetch();
    assertThatProfilesAreEqual(readProfiles, profiles);
  }

  private static void assertThatProfilesAreEqual(
      SubscriptionProfiles actual, SubscriptionProfiles expected) {
    assertThat(actual)
        .usingRecursiveComparison()
        .withComparatorForType(new InstantComparatorIgnoringNanos(), Instant.class)
        .isEqualTo(expected);
  }

  private static class InstantComparatorIgnoringNanos implements Comparator<Instant> {

    @Override
    public int compare(Instant o1, Instant o2) {
      return o1.truncatedTo(MILLIS).compareTo(o2.truncatedTo(MILLIS));
    }
  }
}
