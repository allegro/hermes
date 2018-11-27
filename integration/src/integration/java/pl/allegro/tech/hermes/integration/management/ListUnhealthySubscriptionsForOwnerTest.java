package pl.allegro.tech.hermes.integration.management;

import com.google.common.collect.ImmutableSet;
import static org.assertj.core.api.Assertions.assertThat;
import org.jetbrains.annotations.NotNull;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import pl.allegro.tech.hermes.api.MonitoringDetails;
import static pl.allegro.tech.hermes.api.MonitoringDetails.Severity;
import pl.allegro.tech.hermes.api.OwnerId;
import pl.allegro.tech.hermes.api.Subscription;
import static pl.allegro.tech.hermes.api.SubscriptionHealth.Problem;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.api.UnhealthySubscription;
import pl.allegro.tech.hermes.integration.IntegrationTest;
import pl.allegro.tech.hermes.integration.env.SharedServices;
import pl.allegro.tech.hermes.integration.helper.GraphiteEndpoint;
import static pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder.subscription;

import java.util.List;

public class ListUnhealthySubscriptionsForOwnerTest extends IntegrationTest {

    private Topic topic;
    private GraphiteEndpoint graphiteEndpoint;

    @BeforeMethod
    public void initializeAlways() {
        topic = operations.buildTopic("group", "topic");
        graphiteEndpoint = new GraphiteEndpoint(SharedServices.services().graphiteHttpMock());
    }

    @Test
    public void shouldNotListHealthySubscribtions() {
        // given
        createSubscriptionForOwner("s1", "Team A");
        createSubscriptionForOwner("s2", "Team B");

        // then
        assertThat(listUnhealthySubscriptionsForOwner("Team A")).isEmpty();
    }

    @Test
    public void shouldReturnOnlyUnhealthySubscriptionOfSingleOwner() {
        // given
        createSubscriptionForOwner("ownedSubscription1", "Team A");
        createSubscriptionForOwner("ownedSubscription2", "Team A");
        createSubscriptionForOwner("ownedSubscription3", "Team B");

        graphiteEndpoint.returnMetricForTopic("group", "topic", 100, 50);
        graphiteEndpoint.returnMetricForSubscription("group", "topic", "ownedSubscription1", 100);
        graphiteEndpoint.returnMetricForSubscription("group", "topic", "ownedSubscription2", 50);
        graphiteEndpoint.returnMetricForSubscription("group", "topic", "ownedSubscription3", 100);

        // then
        assertThat(listUnhealthySubscriptionsForOwner("Team A")).containsOnly(
                new UnhealthySubscription("ownedSubscription2", "group.topic", Severity.IMPORTANT, ImmutableSet.of(Problem.SLOW))
        );
    }

    @Test
    public void shouldReportAllUnhealthySubscriptionsForEmptyOwnerSource() {
        // given
        createSubscriptionForOwner("ownedSubscription1", "Team A");
        createSubscriptionForOwner("ownedSubscription2", "Team A");

        graphiteEndpoint.returnMetricForTopic("group", "topic", 100, 50);
        graphiteEndpoint.returnMetricForSubscription("group", "topic", "ownedSubscription1", 100);
        graphiteEndpoint.returnMetricForSubscription("group", "topic", "ownedSubscription2", 50);

        // then
        assertThat(listAllUnhealthySubscriptions()).containsOnly(
                new UnhealthySubscription("ownedSubscription2", "group.topic", Severity.IMPORTANT, ImmutableSet.of(Problem.SLOW))
        );
    }

    @Test
    public void shouldReportUnhealthySubscriptionsDisrespectingSeverity() {
        // given
        createSubscriptionForOwner("ownedSubscription1", "Team A", Severity.CRITICAL);
        createSubscriptionForOwner("ownedSubscription2", "Team A", Severity.IMPORTANT);
        createSubscriptionForOwner("ownedSubscription3", "Team A", Severity.NON_IMPORTANT);

        graphiteEndpoint.returnMetricForTopic("group", "topic", 100, 50);
        graphiteEndpoint.returnMetricForSubscription("group", "topic", "ownedSubscription1", 50);
        graphiteEndpoint.returnMetricForSubscription("group", "topic", "ownedSubscription2", 50);
        graphiteEndpoint.returnMetricForSubscription("group", "topic", "ownedSubscription3", 50);

        // then
        assertThat(listAllUnhealthySubscriptions()).contains(
                new UnhealthySubscription("ownedSubscription1", "group.topic", Severity.CRITICAL, ImmutableSet.of(Problem.SLOW)),
                new UnhealthySubscription("ownedSubscription2", "group.topic", Severity.IMPORTANT, ImmutableSet.of(Problem.SLOW))
        );
    }

    @Test
    public void shouldReportSuspendedSubscriptionAsHealthy() {
        // given
        Subscription s = createSubscriptionForOwner("subscription1", "Team A");

        // when
        s.setState(Subscription.State.SUSPENDED);

        // then
        assertThat(listUnhealthySubscriptionsForOwner("Team A")).isEmpty();
    }

    private Subscription createSubscriptionForOwner(String subscriptionName, String ownerId) {
        return createSubscriptionForOwner(subscriptionName, ownerId, Severity.IMPORTANT);
    }

    private Subscription createSubscriptionForOwner(String subscriptionName, String ownerId, Severity severity) {
        Subscription subscription = subscription(topic, subscriptionName)
                .withEndpoint(HTTP_ENDPOINT_URL)
                .withOwner(ownerId(ownerId))
                .withMonitoringDetails(new MonitoringDetails(severity, ""))
                .build();

        operations.createSubscription(topic, subscription);
        return subscription;
    }

    @NotNull
    private OwnerId ownerId(String ownerId) {
        return new OwnerId("Plaintext", ownerId);
    }

    private List<UnhealthySubscription> listUnhealthySubscriptionsForOwner(String ownerId) {
        return management.unhealthyEndpoint().listUnhealthy("Plaintext", ownerId, true);
    }

    private List<UnhealthySubscription> listUnhealthySubscriptionsDiscrespectingSeverity(String ownerId) {
        return management.unhealthyEndpoint().listUnhealthy("Plaintext", ownerId, false);
    }

    private List<UnhealthySubscription> listAllUnhealthySubscriptions() {
        return management.unhealthyEndpoint().listUnhealthy(null, null, true);
    }
}
