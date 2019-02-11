package pl.allegro.tech.hermes.integration.management;

import com.google.common.collect.ImmutableSet;
import org.jetbrains.annotations.NotNull;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import pl.allegro.tech.hermes.api.MonitoringDetails;
import pl.allegro.tech.hermes.api.OwnerId;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.api.UnhealthySubscription;
import pl.allegro.tech.hermes.integration.IntegrationTest;
import pl.allegro.tech.hermes.integration.env.SharedServices;
import pl.allegro.tech.hermes.integration.helper.GraphiteEndpoint;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.GenericType;
import java.util.List;

import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static pl.allegro.tech.hermes.api.MonitoringDetails.Severity;
import static pl.allegro.tech.hermes.api.SubscriptionHealthProblem.malfunctioning;
import static pl.allegro.tech.hermes.integration.helper.GraphiteEndpoint.subscriptionMetricsStub;
import static pl.allegro.tech.hermes.integration.test.HermesAssertions.assertThat;
import static pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder.subscription;

public class ListUnhealthySubscriptionsForOwnerTest extends IntegrationTest {

    private Topic topic;
    private GraphiteEndpoint graphiteEndpoint;
    private Client httpClient = ClientBuilder.newClient();

    @BeforeMethod
    public void initializeAlways() {
        topic = operations.buildTopic("group", "topic");
        graphiteEndpoint = new GraphiteEndpoint(SharedServices.services().graphiteHttpMock());
    }

    @Test
    public void shouldNotListHealthySubscriptions() {
        // given
        createSubscriptionForOwner("s1", "Team A");
        createSubscriptionForOwner("s2", "Team B");

        // then
        assertThat(listUnhealthySubscriptionsForOwner("Team A")).isEmpty();
        assertThat(listUnhealthySubscriptionsForOwnerAsPlainText("Team A")).isEmpty();
    }

    @Test
    public void shouldReturnOnlyUnhealthySubscriptionOfSingleOwner() {
        // given
        createSubscriptionForOwner("ownedSubscription1", "Team A");
        createSubscriptionForOwner("ownedSubscription2", "Team A");
        createSubscriptionForOwner("ownedSubscription3", "Team B");

        graphiteEndpoint.returnMetricForTopic("group", "topic", 100, 50);
        graphiteEndpoint.returnMetric(subscriptionMetricsStub("group.topic.ownedSubscription1").withRate(100).withStatusRate(500, 0).build());
        graphiteEndpoint.returnMetric(subscriptionMetricsStub("group.topic.ownedSubscription2").withRate(50).withStatusRate(500, 11).build());
        graphiteEndpoint.returnMetric(subscriptionMetricsStub("group.topic.ownedSubscription3").withRate(100).withStatusRate(500, 0).build());

        // then
        assertThat(listUnhealthySubscriptionsForOwner("Team A")).containsOnly(
                new UnhealthySubscription("ownedSubscription2", "group.topic", Severity.IMPORTANT, ImmutableSet.of(malfunctioning(11)))
        );
        assertThat(listUnhealthySubscriptionsForOwnerAsPlainText("Team A")).isEqualTo(
                "ownedSubscription2 - Consuming service returns a lot of 5xx codes, currently 11 5xx/s"
        );
    }

    @Test
    public void shouldReportAllUnhealthySubscriptionsForEmptyOwnerSource() {
        // given
        createSubscriptionForOwner("ownedSubscription1", "Team A");
        createSubscriptionForOwner("ownedSubscription2", "Team A");

        graphiteEndpoint.returnMetricForTopic("group", "topic", 100, 50);
        graphiteEndpoint.returnMetric(subscriptionMetricsStub("group.topic.ownedSubscription1").withRate(100).withStatusRate(500, 0).build());
        graphiteEndpoint.returnMetric(subscriptionMetricsStub("group.topic.ownedSubscription2").withRate(50).withStatusRate(500, 11).build());

        // then
        assertThat(listAllUnhealthySubscriptions()).containsOnly(
                new UnhealthySubscription("ownedSubscription2", "group.topic", Severity.IMPORTANT, ImmutableSet.of(malfunctioning(11)))
        );
        assertThat(listAllUnhealthySubscriptionsAsPlainText()).isEqualTo(
                "ownedSubscription2 - Consuming service returns a lot of 5xx codes, currently 11 5xx/s"
        );
    }

    @Test
    public void shouldReportUnhealthySubscriptionsDisrespectingSeverity() {
        // given
        createSubscriptionForOwner("ownedSubscription1", "Team A", Severity.CRITICAL);
        createSubscriptionForOwner("ownedSubscription2", "Team A", Severity.IMPORTANT);
        createSubscriptionForOwner("ownedSubscription3", "Team A", Severity.NON_IMPORTANT);

        graphiteEndpoint.returnMetricForTopic("group", "topic", 100, 50);
        graphiteEndpoint.returnMetric(subscriptionMetricsStub("group.topic.ownedSubscription1").withRate(50).withStatusRate(500, 11).build());
        graphiteEndpoint.returnMetric(subscriptionMetricsStub("group.topic.ownedSubscription2").withRate(50).withStatusRate(500, 11).build());
        graphiteEndpoint.returnMetric(subscriptionMetricsStub("group.topic.ownedSubscription3").withRate(50).withStatusRate(500, 11).build());

        // then
        assertThat(listUnhealthySubscriptionsDisrespectingSeverity("Team A")).contains(
                new UnhealthySubscription("ownedSubscription1", "group.topic", Severity.CRITICAL, ImmutableSet.of(malfunctioning(11))),
                new UnhealthySubscription("ownedSubscription2", "group.topic", Severity.IMPORTANT, ImmutableSet.of(malfunctioning(11))),
                new UnhealthySubscription("ownedSubscription3", "group.topic", Severity.NON_IMPORTANT, ImmutableSet.of(malfunctioning(11)))
        );
        assertThat(listUnhealthySubscriptionsDisrespectingSeverityAsPlainText("Team A")).isEqualTo(
                "ownedSubscription1 - Consuming service returns a lot of 5xx codes, currently 11 5xx/s\r\n" +
                "ownedSubscription2 - Consuming service returns a lot of 5xx codes, currently 11 5xx/s\r\n" +
                "ownedSubscription3 - Consuming service returns a lot of 5xx codes, currently 11 5xx/s"
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
        assertThat(listUnhealthySubscriptionsForOwnerAsPlainText("Team A")).isEmpty();
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
        return listUnhealthy("Plaintext", ownerId, true);
    }

    private List<UnhealthySubscription> listUnhealthySubscriptionsDisrespectingSeverity(String ownerId) {
        return listUnhealthy("Plaintext", ownerId, false);
    }

    private List<UnhealthySubscription> listAllUnhealthySubscriptions() {
        return listUnhealthy(null, null, true);
    }

    private List<UnhealthySubscription> listUnhealthy(String ownerSourceName, String ownerId, boolean respectMonitoringSeverity) {
        return management.unhealthyEndpoint().listUnhealthy(ownerSourceName, ownerId, respectMonitoringSeverity)
                .readEntity(new GenericType<List<UnhealthySubscription>>() {});
    }

    private String listUnhealthySubscriptionsForOwnerAsPlainText(String ownerId) {
        return listUnhealthyAsPlainText("Plaintext", ownerId, true);
    }

    private String listUnhealthySubscriptionsDisrespectingSeverityAsPlainText(String ownerId) {
        return listUnhealthyAsPlainText("Plaintext", ownerId, false);
    }

    private String listAllUnhealthySubscriptionsAsPlainText() {
        return listUnhealthyAsPlainText(null, null, true);
    }

    private String listUnhealthyAsPlainText(String ownerSourceName, String ownerId, boolean respectMonitoringSeverity) {
        return httpClient.target(MANAGEMENT_ENDPOINT_URL)
                .path("unhealthy")
                .queryParam("ownerSourceName", ownerSourceName)
                .queryParam("ownerId", ownerId)
                .queryParam("respectMonitoringSeverity", respectMonitoringSeverity)
                .request(TEXT_PLAIN)
                .get(String.class);
    }
}
