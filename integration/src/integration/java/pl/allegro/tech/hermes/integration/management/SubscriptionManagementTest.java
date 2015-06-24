package pl.allegro.tech.hermes.integration.management;

import org.assertj.core.api.Assertions;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import pl.allegro.tech.hermes.api.EndpointAddress;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.client.HermesClient;
import pl.allegro.tech.hermes.client.jersey.JerseyHermesSender;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.integration.IntegrationTest;
import pl.allegro.tech.hermes.integration.env.SharedServices;
import pl.allegro.tech.hermes.integration.shame.Unreliable;
import pl.allegro.tech.hermes.test.helper.endpoint.RemoteServiceEndpoint;
import pl.allegro.tech.hermes.test.helper.message.TestMessage;

import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.jayway.awaitility.Awaitility.await;
import static java.net.URI.create;
import static javax.ws.rs.client.ClientBuilder.newClient;
import static pl.allegro.tech.hermes.api.Subscription.Builder.subscription;
import static pl.allegro.tech.hermes.api.SubscriptionPolicy.Builder.subscriptionPolicy;
import static pl.allegro.tech.hermes.api.Topic.Builder.topic;
import static pl.allegro.tech.hermes.client.HermesClientBuilder.hermesClient;
import static pl.allegro.tech.hermes.integration.test.HermesAssertions.assertThat;

public class SubscriptionManagementTest extends IntegrationTest {

    public static final TestMessage MESSAGE = TestMessage.of("hello", "world");

    private RemoteServiceEndpoint remoteService;
    private HermesClient client;

    @BeforeMethod
    public void initializeAlways() {
        remoteService = new RemoteServiceEndpoint(SharedServices.services().serviceMock());
        client = hermesClient(new JerseyHermesSender(newClient())).withURI(create("http://localhost:" + FRONTEND_PORT)).build();
    }

    @Test
    public void shouldCreateSubscriptionWithPendingStatus() {
        // given
        operations.createGroup("subscribeGroup");
        operations.createTopic("subscribeGroup", "topic");

        // when
        Response response = management.subscription().create("subscribeGroup.topic",
                subscription().withName("subscription").withEndpoint(EndpointAddress.of("http://whatever.com")).applyDefaults().build());

        // then
        assertThat(response).hasStatus(Response.Status.CREATED);
        Assertions.assertThat(management.subscription().list("subscribeGroup.topic", false)).containsExactly(
                "subscription");
        Assertions.assertThat(management.subscription().get("subscribeGroup.topic", "subscription").getState())
                .isEqualTo(Subscription.State.PENDING);
    }
    
    @Unreliable
    @Test
    public void shouldSuspendSubscription() {
        // given
        operations.createGroup("suspendSubscriptionGroup");
        operations.createTopic("suspendSubscriptionGroup", "topic");
        operations.createSubscription("suspendSubscriptionGroup", "topic", "subscription", HTTP_ENDPOINT_URL);

        // when
        Response response = management.subscription().updateState(
                "suspendSubscriptionGroup.topic",
                "subscription", Subscription.State.SUSPENDED);

        // then
        assertThat(response).hasStatus(Response.Status.OK);
        Assertions.assertThat(
                management.subscription().get("suspendSubscriptionGroup.topic", "subscription").getState()
        ).isEqualTo(Subscription.State.SUSPENDED);
    }

    @Test
    public void shouldRemoveSubscription() {
        // given
        operations.createGroup("removeSubscriptionGroup");
        operations.createTopic("removeSubscriptionGroup", "topic");
        operations.createSubscription("removeSubscriptionGroup", "topic", "subscription",
                HTTP_ENDPOINT_URL);

        // when
        Response response = management.subscription().remove("removeSubscriptionGroup.topic", "subscription");
        
        // then
        assertThat(response).hasStatus(Response.Status.OK);
        assertThat(management.subscription().list("removeSubscriptionGroup.topic", false)).doesNotContain(
                "subscription");
    }

    @Test
    public void shouldGetEventStatus() throws InterruptedException {
        // given
        operations.createGroup("eventStatus");
        operations.createTopic(topic().withName("eventStatus", "topic").withTrackingEnabled(true).build());

        Subscription subscription = subscription().withName("subscription")
                .withEndpoint(EndpointAddress.of(HTTP_ENDPOINT_URL))
                .withTrackingEnabled(true)
                .withSubscriptionPolicy(subscriptionPolicy().applyDefaults().build())
                .build();

        operations.createSubscription("eventStatus", "topic", subscription);
        remoteService.expectMessages(MESSAGE.body());

        // when
        String messageId = publishMessage("eventStatus.topic", MESSAGE.body());
        remoteService.waitUntilReceived();

        // then
        await().atMost(30, TimeUnit.SECONDS).until(() -> getMessageTrace("eventStatus.topic", "subscription", messageId).size() == 3);

        List<Map<String, String>> traces = getMessageTrace("eventStatus.topic", "subscription", messageId);
        traces.forEach(entry -> assertThat(entry).containsEntry("messageId", messageId));
        assertThat(traces.get(0)).containsEntry("status", "SUCCESS").containsKey("cluster");
        assertThat(traces.get(1)).containsEntry("status", "INFLIGHT").containsKey("cluster");
        assertThat(traces.get(2)).containsEntry("status", "SUCCESS").containsKey("cluster");
        traces.forEach(trace -> assertThat(trace).containsEntry("cluster", Configs.KAFKA_CLUSTER_NAME.getDefaultValue()));
    }

    @Test
    public void shouldReturnSubscriptionsThatAreCurrentlyTrackedForGivenTopic() {
        // given
        TopicName topic = new TopicName("tracked", "topic");
        Subscription subscription = subscription()
                .withName("sub")
                .withTopicName(topic)
                .withEndpoint(new EndpointAddress(HTTP_ENDPOINT_URL))
                .withTrackingEnabled(true).build();
        operations.buildSubscription(topic, subscription);
        operations.createSubscription(topic.getGroupName(), topic.getName(), "sub2", HTTP_ENDPOINT_URL);

        // when
        List<String> tracked = management.subscription().list(topic.qualifiedName(), true);

        // then
        assertThat(tracked).containsExactly(subscription.getName());
    }

    private List<Map<String, String>> getMessageTrace(String topic, String subscription, String messageId) {
        Response response = management.subscription().getMessageTrace(topic, subscription, messageId);
        return response.readEntity(new GenericType<List<Map<String,String>>>() {});
    }

    private String publishMessage(String topic, String body) {
        return client.publish(topic, body).join().getMessageId();
    }
}
