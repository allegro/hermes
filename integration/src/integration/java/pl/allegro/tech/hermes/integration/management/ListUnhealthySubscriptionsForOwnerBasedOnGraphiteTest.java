package pl.allegro.tech.hermes.integration.management;

import com.google.common.collect.ImmutableSet;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.GenericType;
import org.javers.common.collections.Lists;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import pl.allegro.tech.hermes.api.MonitoringDetails;
import pl.allegro.tech.hermes.api.MonitoringDetails.Severity;
import pl.allegro.tech.hermes.api.OwnerId;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.api.UnhealthySubscription;
import pl.allegro.tech.hermes.integration.IntegrationTest;
import pl.allegro.tech.hermes.integration.env.SharedServices;
import pl.allegro.tech.hermes.integration.helper.GraphiteEndpoint;
import pl.allegro.tech.hermes.integration.helper.Waiter;
import pl.allegro.tech.hermes.integration.setup.HermesManagementInstance;
import pl.allegro.tech.hermes.test.helper.endpoint.HermesAPIOperations;
import pl.allegro.tech.hermes.test.helper.endpoint.HermesEndpoints;
import pl.allegro.tech.hermes.test.helper.util.Ports;

import java.util.List;

import static jakarta.ws.rs.core.MediaType.TEXT_PLAIN;
import static org.assertj.core.api.AssertionsForClassTypes.catchThrowable;
import static pl.allegro.tech.hermes.api.SubscriptionHealthProblem.malfunctioning;
import static pl.allegro.tech.hermes.integration.env.SharedServices.services;
import static pl.allegro.tech.hermes.integration.helper.GraphiteEndpoint.subscriptionMetricsStub;
import static pl.allegro.tech.hermes.integration.test.HermesAssertions.assertThat;
import static pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder.subscription;
import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.randomTopic;
import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.topic;

public class ListUnhealthySubscriptionsForOwnerBasedOnGraphiteTest extends IntegrationTest {

    private final Client httpClient = ClientBuilder.newClient();
    private GraphiteEndpoint graphiteEndpoint;
    private HermesAPIOperations apiOperations;
    private String managementUrl;
    private HermesEndpoints customManagement;
    private Waiter hermesWaiter;

    @BeforeClass
    public void initializeOnce() {
        int managementPort = Ports.nextAvailable();
        managementUrl = "http://localhost:" + managementPort + "/";
        HermesManagementInstance hermesManagement = HermesManagementInstance.starter()
                .port(managementPort)
                .withGraphiteExternalStorageEnabled()
                .addKafkaCluster(DC1, kafkaClusterOne.getBootstrapServersForExternalClients())
                .addZookeeperCluster(DC1, hermesZookeeperOne.getConnectionString())
                .schemaRegistry(schemaRegistry.getUrl())
                .replicationFactor(kafkaClusterOne.getAllBrokers().size())
                .uncleanLeaderElectionEnabled(false)
                .start();
        apiOperations = hermesManagement.operations();
        customManagement = new HermesEndpoints(managementUrl, CONSUMER_ENDPOINT_URL);
        hermesWaiter = new Waiter(customManagement, services().zookeeper(), brokerOperations, PRIMARY_KAFKA_CLUSTER_NAME, KAFKA_NAMESPACE);
    }

    @BeforeMethod
    public void initializeAlways() {
        graphiteEndpoint = new GraphiteEndpoint(SharedServices.services().graphiteHttpMock());
    }

    @AfterMethod
    public void cleanup() {
        removeSubscriptions(customManagement, hermesWaiter);
        removeTopics(customManagement, hermesWaiter);
    }

    @Test
    public void shouldNotListHealthySubscriptions() {
        // given
        Topic topic = apiOperations.buildTopic(randomTopic("group", "topic").build());
        createSubscriptionForOwner(topic, "s1", "Team A");
        createSubscriptionForOwner(topic, "s2", "Team B");

        // then
        assertThat(listUnhealthySubscriptionsForOwner("Team A")).isEmpty();
        assertThat(listUnhealthySubscriptionsForOwnerAsPlainText("Team A")).isEmpty();
    }

    @Test
    public void shouldReturnOnlyUnhealthySubscriptionOfSingleOwner() {
        // given
        Topic topic = apiOperations.buildTopic(topic("groupSecond", "topicSecond").build());
        createSubscriptionForOwner(topic, "ownedSubscription1", "Team A");
        final Subscription subscription = createSubscriptionForOwner(topic, "ownedSubscription2", "Team A");
        createSubscriptionForOwner(topic, "ownedSubscription3", "Team B");

        graphiteEndpoint.returnMetricForTopic(topic.getName().getGroupName(), topic.getName().getName(), 100, 50);
        graphiteEndpoint.returnMetric(
                subscriptionMetricsStub(subName(topic, "ownedSubscription1")).withRate(100).withStatusRate(500, 0).build()
        );
        graphiteEndpoint.returnMetric(
                subscriptionMetricsStub(subName(topic, "ownedSubscription2")).withRate(50).withStatusRate(500, 11).build()
        );
        graphiteEndpoint.returnMetric(
                subscriptionMetricsStub(subName(topic, "ownedSubscription3")).withRate(100).withStatusRate(500, 0).build()
        );

        // then
        assertThat(listUnhealthySubscriptionsForOwner("Team A")).containsOnly(
                new UnhealthySubscription("ownedSubscription2", topic.getQualifiedName(), Severity.IMPORTANT,
                        ImmutableSet.of(malfunctioning(11, subscription.getQualifiedName().toString())))
        );

        assertThat(listUnhealthySubscriptionsForOwnerAsPlainText("Team A")).isEqualTo(
                "ownedSubscription2 - Consuming service returns a lot of 5xx codes for subscription "
                        + subscription.getQualifiedName().toString() + ", currently 11 5xx/s"
        );
    }

    @Test
    public void shouldReturnOnlySpecifiedUnhealthySubscription() {
        // given
        Topic topic1 = apiOperations.buildTopic(topic("group", "topic1").build());
        Topic topic2 = apiOperations.buildTopic(topic("group", "topic2").build());
        Topic topic3 = apiOperations.buildTopic(topic("group", "topic3").build());

        createSubscriptionForOwner(topic1, "ownedSubscription1", "Team A");
        final Subscription subscription2 = createSubscriptionForOwner(topic2, "ownedSubscription2", "Team A");
        final Subscription subscription3 = createSubscriptionForOwner(topic3, "ownedSubscription3", "Team A");
        createSubscriptionForOwner(topic1, "ownedSubscription4", "Team B");
        final Subscription subscription5 = createSubscriptionForOwner(topic2, "ownedSubscription5", "Team B");
        final Subscription subscription6 = createSubscriptionForOwner(topic3, "ownedSubscription6", "Team B");

        graphiteEndpoint.returnMetricForTopic(topic1.getName().getGroupName(), topic1.getName().getName(), 100, 50);
        graphiteEndpoint.returnMetricForTopic(topic2.getName().getGroupName(), topic2.getName().getName(), 100, 50);
        graphiteEndpoint.returnMetricForTopic(topic3.getName().getGroupName(), topic3.getName().getName(), 100, 50);

        graphiteEndpoint.returnMetric(
                subscriptionMetricsStub(subName(topic1, "ownedSubscription1")).withRate(100).withStatusRate(500, 0).build()
        );
        graphiteEndpoint.returnMetric(
                subscriptionMetricsStub(subName(topic2, "ownedSubscription2")).withRate(50).withStatusRate(500, 11).build()
        );
        graphiteEndpoint.returnMetric(
                subscriptionMetricsStub(subName(topic3, "ownedSubscription3")).withRate(50).withStatusRate(500, 11).build()
        );

        graphiteEndpoint.returnMetric(
                subscriptionMetricsStub(subName(topic1, "ownedSubscription4")).withRate(100).withStatusRate(500, 0).build()
        );
        graphiteEndpoint.returnMetric(
                subscriptionMetricsStub(subName(topic2, "ownedSubscription5")).withRate(50).withStatusRate(500, 11).build()
        );
        graphiteEndpoint.returnMetric(
                subscriptionMetricsStub(subName(topic3, "ownedSubscription6")).withRate(50).withStatusRate(500, 11).build()
        );

        // then
        assertThat(listUnhealthySubscriptionsForOwner("Team A", Lists.asList(), Lists.asList("group.topic2"))).containsOnly(
                new UnhealthySubscription("ownedSubscription2", "group.topic2", Severity.IMPORTANT,
                        ImmutableSet.of(malfunctioning(11, subscription2.getQualifiedName().toString())))
        );
        assertThat(listUnhealthySubscriptionsForOwnerAsPlainText("Team A", Lists.asList(), Lists.asList("group.topic2"))).isEqualTo(
                "ownedSubscription2 - Consuming service returns a lot of 5xx codes for subscription "
                        + subscription2.getQualifiedName().toString() + ", currently 11 5xx/s"
        );
        assertThat(listAllUnhealthySubscriptions(Lists.asList(), Lists.asList("group.topic2"))).containsOnly(
                new UnhealthySubscription("ownedSubscription2", "group.topic2", Severity.IMPORTANT,
                        ImmutableSet.of(malfunctioning(11, subscription2.getQualifiedName().toString()))),
                new UnhealthySubscription("ownedSubscription5", "group.topic2", Severity.IMPORTANT,
                        ImmutableSet.of(malfunctioning(11, subscription5.getQualifiedName().toString())))
        );
        assertThat(listAllUnhealthySubscriptionsAsPlainText(Lists.asList(), Lists.asList("group.topic2"))).isEqualTo(
                "ownedSubscription2 - Consuming service returns a lot of 5xx codes for subscription "
                        + subscription2.getQualifiedName().toString() + ", currently 11 5xx/s\r\n"
                        + "ownedSubscription5 - Consuming service returns a lot of 5xx codes for subscription "
                        + subscription5.getQualifiedName().toString() + ", currently 11 5xx/s"
        );
        assertThat(listAllUnhealthySubscriptionsAsPlainText(Lists.asList("ownedSubscription2"), Lists.asList("group.topic2"))).isEqualTo(
                "ownedSubscription2 - Consuming service returns a lot of 5xx codes for subscription "
                        + subscription2.getQualifiedName().toString() + ", currently 11 5xx/s"
        );
        assertThat(listAllUnhealthySubscriptions()).containsOnly(
                new UnhealthySubscription("ownedSubscription2", "group.topic2", Severity.IMPORTANT,
                        ImmutableSet.of(malfunctioning(11, subscription2.getQualifiedName().toString()))),
                new UnhealthySubscription("ownedSubscription3", "group.topic3", Severity.IMPORTANT,
                        ImmutableSet.of(malfunctioning(11, subscription3.getQualifiedName().toString()))),
                new UnhealthySubscription("ownedSubscription5", "group.topic2", Severity.IMPORTANT,
                        ImmutableSet.of(malfunctioning(11, subscription5.getQualifiedName().toString()))),
                new UnhealthySubscription("ownedSubscription6", "group.topic3", Severity.IMPORTANT,
                        ImmutableSet.of(malfunctioning(11, subscription6.getQualifiedName().toString())))
        );
        assertThat(listAllUnhealthySubscriptions(Lists.asList("ownedSubscription2", "ownedSubscription3"),
                Lists.asList("group.topic2", "group.topic3"))).containsOnly(
                new UnhealthySubscription("ownedSubscription2", "group.topic2", Severity.IMPORTANT,
                        ImmutableSet.of(malfunctioning(11, subscription2.getQualifiedName().toString()))),
                new UnhealthySubscription("ownedSubscription3", "group.topic3", Severity.IMPORTANT,
                        ImmutableSet.of(malfunctioning(11, subscription3.getQualifiedName().toString())))
        );
    }

    @Test
    public void shouldReportAllUnhealthySubscriptionsForEmptyOwnerSource() {
        // given
        Topic topic = apiOperations.buildTopic(randomTopic("groupFirst", "topicFirst").build());
        createSubscriptionForOwner(topic, "ownedSubscription1", "Team A");
        final Subscription subscription = createSubscriptionForOwner(topic, "ownedSubscription2", "Team A");

        graphiteEndpoint.returnMetricForTopic(topic.getName().getGroupName(), topic.getName().getName(), 100, 50);
        graphiteEndpoint.returnMetric(
                subscriptionMetricsStub(subName(topic, "ownedSubscription1")).withRate(100).withStatusRate(500, 0).build()
        );
        graphiteEndpoint.returnMetric(
                subscriptionMetricsStub(subName(topic, "ownedSubscription2")).withRate(50).withStatusRate(500, 11).build()
        );

        // then
        assertThat(listAllUnhealthySubscriptions()).containsOnly(
                new UnhealthySubscription("ownedSubscription2", topic.getQualifiedName(), Severity.IMPORTANT,
                        ImmutableSet.of(malfunctioning(11, subscription.getQualifiedName().toString())))
        );
        assertThat(listAllUnhealthySubscriptionsAsPlainText()).isEqualTo(
                "ownedSubscription2 - Consuming service returns a lot of 5xx codes for subscription "
                        + subscription.getQualifiedName().toString() + ", currently 11 5xx/s"
        );
    }

    @Test
    public void shouldReportUnhealthySubscriptionsDisrespectingSeverity() {
        // given
        Topic topic = apiOperations.buildTopic(randomTopic("group", "topic").build());
        final Subscription subscription1 = createSubscriptionForOwner(topic, "ownedSubscription1", "Team A", Severity.CRITICAL);
        final Subscription subscription2 = createSubscriptionForOwner(topic, "ownedSubscription2", "Team A", Severity.IMPORTANT);
        final Subscription subscription3 = createSubscriptionForOwner(topic, "ownedSubscription3", "Team A", Severity.NON_IMPORTANT);

        graphiteEndpoint.returnMetricForTopic(topic.getName().getGroupName(), topic.getName().getName(), 100, 50);
        graphiteEndpoint.returnMetric(
                subscriptionMetricsStub(subName(topic, "ownedSubscription1")).withRate(50).withStatusRate(500, 11).build()
        );
        graphiteEndpoint.returnMetric(
                subscriptionMetricsStub(subName(topic, "ownedSubscription2")).withRate(50).withStatusRate(500, 11).build()
        );
        graphiteEndpoint.returnMetric(
                subscriptionMetricsStub(subName(topic, "ownedSubscription3")).withRate(50).withStatusRate(500, 11).build()
        );

        // then
        assertThat(listUnhealthySubscriptionsDisrespectingSeverity("Team A")).contains(
                new UnhealthySubscription("ownedSubscription1", topic.getQualifiedName(), Severity.CRITICAL,
                        ImmutableSet.of(malfunctioning(11, subscription1.getQualifiedName().toString()))),
                new UnhealthySubscription("ownedSubscription2", topic.getQualifiedName(), Severity.IMPORTANT,
                        ImmutableSet.of(malfunctioning(11, subscription2.getQualifiedName().toString()))),
                new UnhealthySubscription("ownedSubscription3", topic.getQualifiedName(), Severity.NON_IMPORTANT,
                        ImmutableSet.of(malfunctioning(11, subscription3.getQualifiedName().toString())))
        );
        assertThat(listUnhealthySubscriptionsDisrespectingSeverityAsPlainText("Team A")).isEqualTo(
                "ownedSubscription1 - Consuming service returns a lot of 5xx codes for subscription "
                        + subscription1.getQualifiedName().toString() + ", currently 11 5xx/s\r\n"
                        + "ownedSubscription2 - Consuming service returns a lot of 5xx codes for subscription "
                        + subscription2.getQualifiedName().toString() + ", currently 11 5xx/s\r\n"
                        + "ownedSubscription3 - Consuming service returns a lot of 5xx codes for subscription "
                        + subscription3.getQualifiedName().toString() + ", currently 11 5xx/s"
        );
    }

    @Test
    public void shouldTimeoutUnhealthySubscriptionsRequest() {
        // given
        Topic topic = apiOperations.buildTopic(randomTopic("group", "topic").build());
        createSubscriptionForOwner(topic, "ownedSubscription1", "Team A", Severity.CRITICAL);
        int graphiteDelay = 1000;
        graphiteEndpoint.returnMetricWithDelay(subscriptionMetricsStub(subName(topic, "ownedSubscription1"))
                .withRate(50)
                .withStatusRate(200, 11).build(), graphiteDelay);

        // when
        long start = System.currentTimeMillis();
        Throwable thrown = catchThrowable(() -> listUnhealthyAsPlainText("Plaintext", "Team A", true, Lists.asList(), Lists.asList()));
        long end = System.currentTimeMillis();

        // then
        assertThat(thrown).isInstanceOf(InternalServerErrorException.class);
        assertThat(end - start < graphiteDelay);

    }

    @Test
    public void shouldReportSuspendedSubscriptionAsHealthy() {
        // given
        Topic topic = apiOperations.buildTopic(randomTopic("group", "topic").build());
        Subscription s = createSubscriptionForOwner(topic, "subscription1", "Team A");

        // when
        s.setState(Subscription.State.SUSPENDED);

        // then
        assertThat(listUnhealthySubscriptionsForOwner("Team A")).isEmpty();
        assertThat(listUnhealthySubscriptionsForOwnerAsPlainText("Team A")).isEmpty();
    }

    private Subscription createSubscriptionForOwner(Topic topic, String subscriptionName, String ownerId) {
        return createSubscriptionForOwner(topic, subscriptionName, ownerId, Severity.IMPORTANT);
    }

    private Subscription createSubscriptionForOwner(Topic topic, String subscriptionName, String ownerId, Severity severity) {
        Subscription subscription = subscription(topic, subscriptionName)
                .withEndpoint(HTTP_ENDPOINT_URL)
                .withOwner(ownerId(ownerId))
                .withMonitoringDetails(new MonitoringDetails(severity, ""))
                .build();

        apiOperations.createSubscription(topic, subscription);
        return subscription;
    }

    private OwnerId ownerId(String ownerId) {
        return new OwnerId("Plaintext", ownerId);
    }

    private List<UnhealthySubscription> listUnhealthySubscriptionsForOwner(String ownerId) {
        return listUnhealthy("Plaintext", ownerId, true, Lists.asList(), Lists.asList());
    }

    private List<UnhealthySubscription> listUnhealthySubscriptionsForOwner(String ownerId, List<String> subscriptionNames,
                                                                           List<String> qualifiedTopicNames) {
        return listUnhealthy("Plaintext", ownerId, true, subscriptionNames, qualifiedTopicNames);
    }

    private List<UnhealthySubscription> listUnhealthySubscriptionsDisrespectingSeverity(String ownerId) {
        return listUnhealthy("Plaintext", ownerId, false, Lists.asList(), Lists.asList());
    }

    private List<UnhealthySubscription> listAllUnhealthySubscriptions() {
        return listUnhealthy(null, null, true, Lists.asList(), Lists.asList());
    }

    private List<UnhealthySubscription> listAllUnhealthySubscriptions(List<String> subscriptionNames, List<String> qualifiedTopicNames) {
        return listUnhealthy(null, null, true, subscriptionNames, qualifiedTopicNames);
    }

    private List<UnhealthySubscription> listUnhealthy(String ownerSourceName, String ownerId, boolean respectMonitoringSeverity,
                                                      List<String> subscriptionNames, List<String> qualifiedTopicNames) {
        return customManagement.unhealthyEndpoint()
                .listUnhealthy(ownerSourceName, ownerId, respectMonitoringSeverity, subscriptionNames, qualifiedTopicNames)
                .readEntity(new GenericType<>() {
                });
    }

    private String listUnhealthySubscriptionsForOwnerAsPlainText(String ownerId) {
        return listUnhealthyAsPlainText("Plaintext", ownerId, true, Lists.asList(), Lists.asList());
    }

    private String listUnhealthySubscriptionsForOwnerAsPlainText(String ownerId, List<String> subscriptionNames,
                                                                 List<String> qualifiedTopicNames) {
        return listUnhealthyAsPlainText("Plaintext", ownerId, true, subscriptionNames, qualifiedTopicNames);
    }

    private String listUnhealthySubscriptionsDisrespectingSeverityAsPlainText(String ownerId) {
        return listUnhealthyAsPlainText("Plaintext", ownerId, false, Lists.asList(), Lists.asList());
    }

    private String listAllUnhealthySubscriptionsAsPlainText() {
        return listUnhealthyAsPlainText(null, null, true, Lists.asList(), Lists.asList());
    }

    private String listAllUnhealthySubscriptionsAsPlainText(List<String> subscriptionNames, List<String> qualifiedTopicNames) {
        return listUnhealthyAsPlainText(null, null, true, subscriptionNames, qualifiedTopicNames);
    }

    private String listUnhealthyAsPlainText(String ownerSourceName, String ownerId, boolean respectMonitoringSeverity,
                                            List<String> subscriptionNames, List<String> qualifiedTopicNames) {
        return httpClient.target(managementUrl)
                .path("unhealthy")
                .queryParam("ownerSourceName", ownerSourceName)
                .queryParam("ownerId", ownerId)
                .queryParam("respectMonitoringSeverity", respectMonitoringSeverity)
                .queryParam("subscriptionNames", (Object[]) subscriptionNames.toArray(new String[0]))
                .queryParam("qualifiedTopicNames", (Object[]) qualifiedTopicNames.toArray(new String[0]))
                .request(TEXT_PLAIN)
                .get(String.class);
    }

    private String subName(Topic topic, String rawSubscriptionName) {
        return topic.getQualifiedName() + "." + rawSubscriptionName;
    }
}
