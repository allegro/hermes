package pl.allegro.tech.hermes.integration.management;

import com.google.common.collect.ImmutableMap;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import pl.allegro.tech.hermes.api.ContentType;
import pl.allegro.tech.hermes.api.DeliveryType;
import pl.allegro.tech.hermes.api.EndpointAddress;
import pl.allegro.tech.hermes.api.PatchData;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.SubscriptionHealth;
import pl.allegro.tech.hermes.api.SubscriptionPolicy;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.api.TrackingMode;
import pl.allegro.tech.hermes.client.HermesClient;
import pl.allegro.tech.hermes.client.jersey.JerseyHermesSender;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.integration.IntegrationTest;
import pl.allegro.tech.hermes.integration.env.SharedServices;
import pl.allegro.tech.hermes.integration.helper.GraphiteEndpoint;
import pl.allegro.tech.hermes.integration.shame.Unreliable;
import pl.allegro.tech.hermes.test.helper.endpoint.RemoteServiceEndpoint;
import pl.allegro.tech.hermes.test.helper.message.TestMessage;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static com.jayway.awaitility.Awaitility.await;
import static java.net.URI.create;
import static javax.ws.rs.client.ClientBuilder.newClient;
import static pl.allegro.tech.hermes.api.PatchData.patchData;
import static pl.allegro.tech.hermes.api.SubscriptionHealth.Status.UNHEALTHY;
import static pl.allegro.tech.hermes.api.SubscriptionHealthProblem.malfunctioning;
import static pl.allegro.tech.hermes.client.HermesClientBuilder.hermesClient;
import static pl.allegro.tech.hermes.integration.helper.GraphiteEndpoint.subscriptionMetricsStub;
import static pl.allegro.tech.hermes.integration.test.HermesAssertions.assertThat;
import static pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder.subscription;
import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.topic;

public class SubscriptionManagementTest extends IntegrationTest {

    public static final TestMessage MESSAGE = TestMessage.of("hello", "world");

    private Client httpClient = ClientBuilder.newClient();

    private RemoteServiceEndpoint remoteService;
    private HermesClient client;
    private GraphiteEndpoint graphiteEndpoint;

    @BeforeMethod
    public void initializeAlways() {
        remoteService = new RemoteServiceEndpoint(SharedServices.services().serviceMock());
        client = hermesClient(new JerseyHermesSender(newClient())).withURI(create("http://localhost:" + FRONTEND_PORT)).build();
        graphiteEndpoint = new GraphiteEndpoint(SharedServices.services().graphiteHttpMock());
    }

    @Test
    public void shouldCreateSubscriptionWithActiveStatus() {
        // given
        Topic topic = operations.buildTopic("subscribeGroup", "topic");

        // when
        Response response = management.subscription().create(
                topic.getQualifiedName(),
                subscription("subscribeGroup.topic", "subscription").build()
        );

        // then
        assertThat(response).hasStatus(Response.Status.CREATED);
        wait.untilSubscriptionCreated(topic, "subscription", false);
        assertThat(management.subscription().list(topic.getQualifiedName(), false)).containsExactly("subscription");
        wait.untilSubscriptionIsActivated(topic, "subscription");
    }

    @Test
    public void shouldNotCreateSubscriptionWithoutTopicName() {
        // given
        operations.buildTopic("invalidGroup", "topic");

        // when
        Response response = httpClient.target(MANAGEMENT_ENDPOINT_URL + "topics/invalidGroup.topic/subscriptions")
                .request()
                .post(Entity.json("{\"name\": \"subscription\"}"));

        // then
        assertThat(response).hasStatus(Response.Status.BAD_REQUEST);
    }

    @Test
    public void shouldSuspendSubscription() {
        // given
        Topic topic = operations.buildTopic("suspendSubscriptionGroup", "topic");
        operations.createSubscription(topic, "subscription", HTTP_ENDPOINT_URL);
        wait.untilSubscriptionIsActivated(topic, "subscription");

        // when
        Response response = management.subscription().updateState(
                topic.getQualifiedName(),
                "subscription", Subscription.State.SUSPENDED);

        // then
        assertThat(response).hasStatus(Response.Status.OK);
        wait.untilSubscriptionIsSuspended(topic, "subscription");
    }

    @Test
    public void shouldUpdateSubscriptionEndpoint() throws Throwable {
        //given
        Topic topic = operations.buildTopic("updateSubscriptionEndpointAddressGroup", "topic");
        operations.createSubscription(topic, "subscription", HTTP_ENDPOINT_URL + "v1/");
        wait.untilSubscriptionIsActivated(topic, "subscription");

        // when
        Response response = management.subscription().update(
                topic.getQualifiedName(),
                "subscription",
                patchData().set("endpoint", EndpointAddress.of(HTTP_ENDPOINT_URL)).build()
        );

        // then
        assertThat(response).hasStatus(Response.Status.OK);
        wait.untilSubscriptionEndpointAddressChanged(topic, "subscription", EndpointAddress.of(HTTP_ENDPOINT_URL));

        remoteService.expectMessages(MESSAGE.body());
        publishMessage(topic.getQualifiedName(), MESSAGE.body());
        remoteService.waitUntilReceived();
    }

    @Test
    public void shouldUpdateSubscriptionPolicy() {
        // given
        Topic topic = operations.buildTopic("updateSubscriptionPolicy", "topic");
        operations.createSubscription(topic, "subscription", HTTP_ENDPOINT_URL + "v1/");
        wait.untilSubscriptionIsActivated(topic, "subscription");

        PatchData patchData = patchData().set("subscriptionPolicy", ImmutableMap.builder()
                    .put("inflightSize", 100)
                    .put("messageBackoff", 100)
                    .put("messageTtl", 3600)
                    .put("rate", 300)
                    .put("requestTimeout", 1000)
                    .put("retryClientErrors", false)
                    .put("sendingDelay", 1000)
                    .build())
                .build();

        // when
        Response response = management.subscription().update(
                topic.getQualifiedName(),
                "subscription",
                patchData
        );

        // then
        assertThat(response).hasStatus(Response.Status.OK);
        SubscriptionPolicy policy = management.subscription().get(topic.getQualifiedName(), "subscription").getSerialSubscriptionPolicy();
        assertThat(policy.getInflightSize()).isEqualTo(100);
        assertThat(policy.getMessageBackoff()).isEqualTo(100);
        assertThat(policy.getMessageTtl()).isEqualTo(3600);
        assertThat(policy.getRate()).isEqualTo(300);
        assertThat(policy.getRequestTimeout()).isEqualTo(1000);
        assertThat(policy.isRetryClientErrors()).isFalse();
        assertThat(policy.getSendingDelay()).isEqualTo(1000);
    }

    @Test
    public void shouldRemoveSubscription() {
        // given
        Topic topic = operations.buildTopic("removeSubscriptionGroup", "topic");
        operations.createSubscription(topic, "subscription", HTTP_ENDPOINT_URL);

        // when
        Response response = management.subscription().remove(topic.getQualifiedName(), "subscription");

        // then
        assertThat(response).hasStatus(Response.Status.OK);
        assertThat(management.subscription().list(topic.getQualifiedName(), false)).doesNotContain(
                "subscription");
    }

    @Unreliable
    @Test(enabled = false)
    public void shouldGetEventStatus() throws InterruptedException {
        // given
        Topic topic = operations.buildTopic(topic("eventStatus", "topic").withContentType(ContentType.JSON)
                .withTrackingEnabled(true).build());

        Subscription subscription = subscription("eventStatus.topic", "subscription", HTTP_ENDPOINT_URL)
                .withTrackingMode(TrackingMode.TRACK_ALL)
                .build();

        operations.createSubscription(topic, subscription);
        remoteService.expectMessages(MESSAGE.body());

        // when
        String messageId = publishMessage(topic.getQualifiedName(), MESSAGE.body());
        remoteService.waitUntilReceived();

        // then
        await().atMost(30, TimeUnit.SECONDS).until(() -> getMessageTrace(topic.getQualifiedName(), "subscription", messageId).size() == 3);

        List<Map<String, String>> traces = getMessageTrace(topic.getQualifiedName(), "subscription", messageId);
        traces.forEach(entry -> assertThat(entry).containsEntry("messageId", messageId));
        assertThat(traces.get(0)).containsEntry("status", "SUCCESS").containsKey("cluster");
        assertThat(traces.get(1)).containsEntry("status", "INFLIGHT").containsKey("cluster");
        assertThat(traces.get(2)).containsEntry("status", "SUCCESS").containsKey("cluster");
        traces.forEach(trace -> assertThat(trace).containsEntry("cluster", Configs.KAFKA_CLUSTER_NAME.getDefaultValue()));
    }

    @Test
    public void shouldReturnSubscriptionsThatAreCurrentlyTrackedForGivenTopic() {
        // given
        Topic topic = operations.buildTopic("tracked", "topic");
        Subscription subscription = subscription("tracked.topic", "subscription", HTTP_ENDPOINT_URL)
                .withTrackingMode(TrackingMode.TRACK_ALL).build();
        operations.createSubscription(topic, subscription);
        operations.createSubscription(topic, "sub2", HTTP_ENDPOINT_URL);

        // when
        List<String> tracked = management.subscription().list(topic.getQualifiedName(), true);

        // then
        assertThat(tracked).containsExactly(subscription.getName());
    }

    @Test
    public void shouldReturnsTrackedAndNotSuspendedSubscriptionsForGivenTopic() {

        // given
        Topic topic = operations.buildTopic("queried", "topic");
        operations.createSubscription(topic, subscription("queried.topic", "sub1").withTrackingMode(TrackingMode.TRACK_ALL).build());
        operations.createSubscription(topic, subscription("queried.topic", "sub2").withTrackingMode(TrackingMode.TRACK_ALL).build());
        operations.createSubscription(topic, subscription("queried.topic", "sub3").build());
        operations.suspendSubscription(topic, "sub2");

        // and
        String query = "{\"query\": {\"trackingEnabled\": \"true\", \"state\": {\"ne\": \"SUSPENDED\"}}}";

        // when
        List<String> tracked = management.subscription().queryList(topic.getQualifiedName(), query);

        // then
        assertThat(tracked).containsExactly("sub1");
    }

    @Test
    public void shouldNotAllowSubscriptionNameToContainDollarSign() {
        // given
        Topic topic = operations.buildTopic("dollar", "topic");

        Stream.of("$name", "na$me", "name$").forEach(name -> {
            // when
            Response response = management.subscription().create(topic.getQualifiedName(), subscription("dollar.topic", name).build());
            // then
            assertThat(response).hasStatus(Response.Status.BAD_REQUEST);
        });
    }

    @Test
    public void shouldReturnHealthyStatusForAHealthySubscription() {
        // given
        String groupName = "healthHealthy";
        String topicName = "topic";
        String subscriptionName = "subscription";

        // and
        Topic topic = operations.buildTopic(groupName, topicName);
        operations.createSubscription(topic, subscriptionName, HTTP_ENDPOINT_URL);
        graphiteEndpoint.returnMetricForTopic(groupName, topicName, 100, 100);
        graphiteEndpoint.returnMetric(subscriptionMetricsStub("healthHealthy.topic.subscription").withRate(100).build());

        // when
        SubscriptionHealth subscriptionHealth = management.subscription().getHealth(topic.getQualifiedName(), subscriptionName);

        // then
        assertThat(subscriptionHealth).isEqualTo(SubscriptionHealth.HEALTHY);
    }

    @Test
    public void shouldReturnUnhealthyStatusWithAProblemForMalfunctioningSubscription() {
        // given
        String groupName = "healthUnhealthy";
        String topicName = "topic";
        String subscriptionName = "subscription";

        // and
        Topic topic = operations.buildTopic(groupName, topicName);
        operations.createSubscription(topic, subscriptionName, HTTP_ENDPOINT_URL);
        graphiteEndpoint.returnMetricForTopic(groupName, topicName, 100, 50);
        graphiteEndpoint.returnMetric(subscriptionMetricsStub("healthUnhealthy.topic.subscription").withRate(50).withStatusRate(500, 11).build());

        // when
        SubscriptionHealth subscriptionHealth = management.subscription().getHealth(topic.getQualifiedName(), subscriptionName);

        // then
        assertThat(subscriptionHealth.getStatus()).isEqualTo(UNHEALTHY);
        assertThat(subscriptionHealth.getProblems()).containsOnly(malfunctioning(11));
    }

    @Test
    public void shouldReturnNoDataStatusWhenGraphiteRespondsWithAnError() {
        // given
        String groupName = "healthNoData";
        String topicName = "topic";
        String subscriptionName = "subscription";

        // and
        Topic topic = operations.buildTopic(groupName, topicName);
        operations.createSubscription(topic, subscriptionName, HTTP_ENDPOINT_URL);
        graphiteEndpoint.returnServerErrorForAllTopics();
        graphiteEndpoint.returnMetric(subscriptionMetricsStub("healthUnhealthy.topic.subscription").withRate(100).build());

        // when
        SubscriptionHealth subscriptionHealth = management.subscription().getHealth(topic.getQualifiedName(), subscriptionName);

        // then
        assertThat(subscriptionHealth).isEqualTo(SubscriptionHealth.NO_DATA);
    }

    @Test
    public void shouldNotAllowSubscriptionWithBatchDeliveryAndAvroContentType() {
        // given
        Topic topic = operations.buildTopic("subscribeGroup", "topic");
        Subscription subscription = subscription("subscribeGroup.topic", "subscription")
                .withDeliveryType(DeliveryType.BATCH)
                .withContentType(ContentType.AVRO)
                .build();

        // when
        Response response = management.subscription().create(
                topic.getQualifiedName(),
                subscription
        );

        // then
        assertThat(response).hasStatus(Response.Status.BAD_REQUEST);
    }

    private List<Map<String, String>> getMessageTrace(String topic, String subscription, String messageId) {
        Response response = management.subscription().getMessageTrace(topic, subscription, messageId);
        return response.readEntity(new GenericType<List<Map<String, String>>>() {
        });
    }

    private String publishMessage(String topic, String body) {
        return client.publish(topic, body).join().getMessageId();
    }
}
