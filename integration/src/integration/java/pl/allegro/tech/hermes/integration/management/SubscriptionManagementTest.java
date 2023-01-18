package pl.allegro.tech.hermes.integration.management;

import com.google.common.collect.ImmutableMap;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import pl.allegro.tech.hermes.api.ConsumerGroup;
import pl.allegro.tech.hermes.api.ContentType;
import pl.allegro.tech.hermes.api.DeliveryType;
import pl.allegro.tech.hermes.api.EndpointAddress;
import pl.allegro.tech.hermes.api.OwnerId;
import pl.allegro.tech.hermes.api.PatchData;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.SubscriptionHealth;
import pl.allegro.tech.hermes.api.SubscriptionPolicy;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.api.TopicPartition;
import pl.allegro.tech.hermes.api.TrackingMode;
import pl.allegro.tech.hermes.client.HermesClient;
import pl.allegro.tech.hermes.client.jersey.JerseyHermesSender;
import pl.allegro.tech.hermes.consumers.config.KafkaProperties;
import pl.allegro.tech.hermes.integration.IntegrationTest;
import pl.allegro.tech.hermes.integration.env.SharedServices;
import pl.allegro.tech.hermes.integration.helper.GraphiteEndpoint;
import pl.allegro.tech.hermes.integration.shame.Unreliable;
import pl.allegro.tech.hermes.management.TestSecurityProvider;
import pl.allegro.tech.hermes.test.helper.endpoint.RemoteServiceEndpoint;
import pl.allegro.tech.hermes.test.helper.message.TestMessage;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;

import static com.jayway.awaitility.Awaitility.await;
import static java.net.URI.create;
import static javax.ws.rs.client.ClientBuilder.newClient;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static javax.ws.rs.core.Response.Status.OK;
import static pl.allegro.tech.hermes.api.PatchData.patchData;
import static pl.allegro.tech.hermes.api.SubscriptionHealth.Status.UNHEALTHY;
import static pl.allegro.tech.hermes.api.SubscriptionHealthProblem.malfunctioning;
import static pl.allegro.tech.hermes.client.HermesClientBuilder.hermesClient;
import static pl.allegro.tech.hermes.integration.helper.GraphiteEndpoint.subscriptionMetricsStub;
import static pl.allegro.tech.hermes.integration.test.HermesAssertions.assertThat;
import static pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder.subscription;
import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.randomTopic;

public class SubscriptionManagementTest extends IntegrationTest {

    public static final TestMessage MESSAGE = TestMessage.of("hello", "world");

    private final Client httpClient = ClientBuilder.newClient();

    private RemoteServiceEndpoint remoteService;
    private HermesClient client;
    private GraphiteEndpoint graphiteEndpoint;

    @BeforeMethod
    public void initializeAlways() {
        remoteService = new RemoteServiceEndpoint(SharedServices.services().serviceMock());
        client = hermesClient(new JerseyHermesSender(newClient())).withURI(create("http://localhost:" + FRONTEND_PORT)).build();
        graphiteEndpoint = new GraphiteEndpoint(SharedServices.services().graphiteHttpMock());
    }

    @AfterMethod
    public void cleanup() {
        TestSecurityProvider.reset();
    }

    @Test
    public void shouldEmmitAuditEventWhenSubscriptionCreated() {
        //given
        Topic topic = operations.buildTopic(randomTopic("subscribeGroup", "topic").build());

        //when
        management.subscription().create(topic.getQualifiedName(), subscription(topic, "someSubscription").build());

        //then
        assertThat(
                auditEvents.getLastRequest().getBodyAsString()
        ).contains("CREATED", "someSubscription");
    }

    @Test
    public void shouldReturnSubscription() {
        //given
        Topic topic = operations.buildTopic(randomTopic("subscribeGroup", "topic").build());
        String subName = "checkFieldSub";
        operations.createSubscription(topic,
                subscription(topic, subName)
                        .build());

        //when
        Subscription subscription = management.subscription()
                .get(topic.getQualifiedName(), subName);

        //then
        assertThat(subscription.getName()).isEqualTo(subName);
        assertThat(subscription.isAutoDeleteWithTopicEnabled()).isFalse();
        assertThat(subscription.getQualifiedTopicName()).isEqualTo(topic.getQualifiedName());
        assertThat(subscription.isTrackingEnabled()).isFalse();
    }

    @Test
    public void shouldEmmitAuditEventWhenSubscriptionRemoved() {
        //given
        Topic topic = operations.buildTopic(randomTopic("subscribeGroup2", "topic").build());
        operations.createSubscription(topic, subscription(topic, "anotherSubscription").build());

        //when
        management.subscription().remove(topic.getQualifiedName(), "anotherSubscription");

        //then
        assertThat(
                auditEvents.getLastRequest().getBodyAsString()
        ).contains("REMOVED", "anotherSubscription");
    }

    @Test
    public void shouldEmmitAuditEventWhenSubscriptionEndpointUpdated() {
        //given
        Topic topic = operations.buildTopic(randomTopic("subscribeGroup3", "topic").build());
        operations.createSubscription(topic, subscription(topic, "anotherOneSubscription").build());

        //when
        management.subscription().update(topic.getQualifiedName(), "anotherOneSubscription",
                patchData().set("endpoint", EndpointAddress.of(remoteService.getUrl())).build());

        //then
        assertThat(
                auditEvents.getLastRequest().getBodyAsString()
        ).contains("UPDATED", "anotherOneSubscription");
    }

    @Test
    public void shouldCreateSubscriptionWithActiveStatus() {
        // given
        Topic topic = operations.buildTopic(randomTopic("subscribeGroup", "topic").build());

        // when
        Response response = management.subscription().create(
                topic.getQualifiedName(),
                subscription(topic.getQualifiedName(), "subscription").build()
        );

        // then
        assertThat(response).hasStatus(Response.Status.CREATED);
        wait.untilSubscriptionCreated(topic, "subscription", false);
        assertThat(management.subscription().list(topic.getQualifiedName(), false)).containsExactly("subscription");
        wait.untilSubscriptionIsActivated(topic, "subscription");
    }

    @Test
    public void shouldNotRemoveSubscriptionAfterItsRecreation() {
        // given
        Topic topic = operations.buildTopic(randomTopic("subscribeGroup", "topic").build());
        Subscription subscription = subscription(topic, "sub").build();
        Response response = management.subscription().create(topic.getQualifiedName(), subscription);
        assertThat(response).hasStatus(Response.Status.CREATED);

        // when
        Response errorResponse = management.subscription().create(topic.getQualifiedName(), subscription);

        // then
        assertThat(errorResponse).hasStatus(Response.Status.BAD_REQUEST);
        assertThat(
                management.subscription().get(topic.getQualifiedName(), subscription.getName()).getName()
        ).isEqualTo("sub");
    }

    @Test
    public void shouldNotCreateSubscriptionWithoutTopicName() {
        // given
        Topic topic = operations.buildTopic(randomTopic("invalidGroup", "topic").build());

        // when
        Response response = httpClient.target(MANAGEMENT_ENDPOINT_URL + "topics/" + topic.getQualifiedName() + "/subscriptions")
                .request()
                .post(Entity.json("{\"name\": \"subscription\"}"));

        // then
        assertThat(response).hasStatus(Response.Status.BAD_REQUEST);
    }

    @Test
    public void shouldSuspendSubscription() {
        // given
        Topic topic = operations.buildTopic(randomTopic("suspendSubscriptionGroup", "topic").build());
        operations.createSubscription(topic, "subscription", remoteService.getUrl());
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
    public void shouldUpdateSubscriptionEndpoint() {
        //given
        Topic topic = operations.buildTopic(randomTopic("updateSubscriptionEndpointAddressGroup", "topic").build());
        operations.createSubscription(topic, "subscription", remoteService.getUrl().toString() + "v1/");
        wait.untilSubscriptionIsActivated(topic, "subscription");

        // when
        Response response = management.subscription().update(
                topic.getQualifiedName(),
                "subscription",
                patchData().set("endpoint", EndpointAddress.of(remoteService.getUrl())).build()
        );

        // then
        assertThat(response).hasStatus(Response.Status.OK);
        wait.untilSubscriptionEndpointAddressChanged(topic, "subscription", EndpointAddress.of(remoteService.getUrl()));

        publishMessage(topic.getQualifiedName(), MESSAGE.body());
        auditEvents.getLastRequest();
    }

    @Test
    public void shouldUpdateSubscriptionPolicy() {
        // given
        Topic topic = operations.buildTopic(randomTopic("updateSubscriptionPolicy", "topic").build());
        operations.createSubscription(topic, "subscription", remoteService.getUrl().toString() + "v1/");
        wait.untilSubscriptionIsActivated(topic, "subscription");

        PatchData patchData = patchData().set("subscriptionPolicy", ImmutableMap.builder()
                .put("inflightSize", 100)
                .put("messageBackoff", 100)
                .put("messageTtl", 3600)
                .put("rate", 300)
                .put("requestTimeout", 1000)
                .put("socketTimeout", 3000)
                .put("retryClientErrors", false)
                .put("sendingDelay", 1000)
                .build()
        ).build();

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
        assertThat(policy.getSocketTimeout()).isEqualTo(3000);
        assertThat(policy.isRetryClientErrors()).isFalse();
        assertThat(policy.getSendingDelay()).isEqualTo(1000);
    }

    @Test
    public void shouldRemoveSubscription() {
        // given
        Topic topic = operations.buildTopic(randomTopic("removeSubscriptionGroup", "topic").build());
        operations.createSubscription(topic, "subscription", remoteService.getUrl());

        // when
        Response response = management.subscription().remove(topic.getQualifiedName(), "subscription");

        // then
        assertThat(response).hasStatus(Response.Status.OK);
        assertThat(management.subscription().list(topic.getQualifiedName(), false)).doesNotContain(
                "subscription");
    }

    @Unreliable
    @Test(enabled = false)
    public void shouldGetEventStatus() {
        // given
        final Topic topic = operations.buildTopic(randomTopic("eventStatus", "topic").withContentType(ContentType.JSON)
                .withTrackingEnabled(true).build());
        final String clusterName = new KafkaProperties().getClusterName();

        final Subscription subscription = subscription(topic.getQualifiedName(), "subscription", remoteService.getUrl())
                .withTrackingMode(TrackingMode.TRACK_ALL)
                .build();

        operations.createSubscription(topic, subscription);

        // when
        String messageId = publishMessage(topic.getQualifiedName(), MESSAGE.body());
        auditEvents.getLastRequest();

        // then
        await().atMost(30, TimeUnit.SECONDS).until(() -> getMessageTrace(topic.getQualifiedName(), "subscription", messageId).size() == 3);

        List<Map<String, String>> traces = getMessageTrace(topic.getQualifiedName(), "subscription", messageId);
        traces.forEach(entry -> assertThat(entry).containsEntry("messageId", messageId));
        assertThat(traces.get(0)).containsEntry("status", "SUCCESS").containsKey("cluster");
        assertThat(traces.get(1)).containsEntry("status", "INFLIGHT").containsKey("cluster");
        assertThat(traces.get(2)).containsEntry("status", "SUCCESS").containsKey("cluster");
        traces.forEach(trace -> assertThat(trace).containsEntry("cluster", clusterName));
    }

    @Test
    public void shouldReturnSubscriptionsThatAreCurrentlyTrackedForGivenTopic() {
        // given
        Topic topic = operations.buildTopic(randomTopic("tracked", "topic").build());
        Subscription subscription = subscription(topic.getQualifiedName(), "subscription", remoteService.getUrl())
                .withTrackingMode(TrackingMode.TRACK_ALL).build();
        operations.createSubscription(topic, subscription);
        operations.createSubscription(topic, "sub2", remoteService.getUrl());

        // when
        List<String> tracked = management.subscription().list(topic.getQualifiedName(), true);

        // then
        assertThat(tracked).containsExactly(subscription.getName());
    }

    @Test
    public void shouldReturnsTrackedAndNotSuspendedSubscriptionsForGivenTopic() {

        // given
        Topic topic = operations.buildTopic(randomTopic("queried", "topic").build());
        operations.createSubscription(
                topic, subscription(topic.getQualifiedName(), "sub1").withTrackingMode(TrackingMode.TRACK_ALL).build()
        );
        operations.createSubscription(
                topic, subscription(topic.getQualifiedName(), "sub2").withTrackingMode(TrackingMode.TRACK_ALL).build()
        );
        operations.createSubscription(topic, subscription(topic.getQualifiedName(), "sub3").build());
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
        Topic topic = operations.buildTopic(randomTopic("dollar", "topic").build());

        Stream.of("$name", "na$me", "name$").forEach(name -> {
            // when
            Response response = management.subscription().create(
                    topic.getQualifiedName(), subscription(topic.getQualifiedName(), name).build()
            );
            // then
            assertThat(response).hasStatus(Response.Status.BAD_REQUEST);
        });
    }

    @Test
    public void shouldReturnHealthyStatusForAHealthySubscription() {
        // given
        Topic topic = randomTopic("healthHealthy", "topic").build();
        String subscriptionName = "subscription";

        // and
        operations.buildTopic(topic);
        operations.createSubscription(topic, subscriptionName, remoteService.getUrl());
        graphiteEndpoint.returnMetricForTopic(topic.getName().getGroupName(), topic.getName().getName(), 100, 100);
        graphiteEndpoint.returnMetric(subscriptionMetricsStub(topic.getQualifiedName() + ".subscription").withRate(100).build());

        // when
        SubscriptionHealth subscriptionHealth = management.subscription().getHealth(topic.getQualifiedName(), subscriptionName);

        // then
        assertThat(subscriptionHealth).isEqualTo(SubscriptionHealth.HEALTHY);
    }

    @Test
    public void shouldReturnUnhealthyStatusWithAProblemForMalfunctioningSubscription() {
        // given
        Topic topic = randomTopic("healthUnhealthy", "topic").build();
        String subscriptionName = "subscription";

        // and
        operations.buildTopic(topic);
        operations.createSubscription(topic, subscriptionName, remoteService.getUrl());
        graphiteEndpoint.returnMetricForTopic(topic.getName().getGroupName(), topic.getName().getName(), 100, 50);
        graphiteEndpoint.returnMetric(subscriptionMetricsStub(topic.getQualifiedName() + ".subscription")
                .withRate(50)
                .withStatusRate(500, 11).build());

        // when
        SubscriptionHealth subscriptionHealth = management.subscription().getHealth(topic.getQualifiedName(), subscriptionName);

        // then
        assertThat(subscriptionHealth.getStatus()).isEqualTo(UNHEALTHY);
        assertThat(subscriptionHealth.getProblems()).containsOnly(malfunctioning(11, topic.getQualifiedName() + "$subscription"));
    }

    @Test
    public void shouldReturnNoDataStatusWhenGraphiteRespondsWithAnError() {
        // given
        String topicName = "topic";
        Topic topic = randomTopic("healthNoData", "topic").build();
        String subscriptionName = "subscription";

        // and
        operations.buildTopic(topic);
        operations.createSubscription(topic, subscriptionName, remoteService.getUrl());
        graphiteEndpoint.returnServerErrorForAllTopics();
        graphiteEndpoint.returnMetric(subscriptionMetricsStub(topic.getQualifiedName() + ".subscription").withRate(100).build());

        // when
        SubscriptionHealth subscriptionHealth = management.subscription().getHealth(topic.getQualifiedName(), subscriptionName);

        // then
        assertThat(subscriptionHealth).isEqualTo(SubscriptionHealth.NO_DATA);
    }

    @Test
    public void shouldNotAllowSubscriptionWithBatchDeliveryAndAvroContentType() {
        // given
        Topic topic = operations.buildTopic(randomTopic("subscribeGroup", "topic").build());
        Subscription subscription = subscription(topic.getQualifiedName(), "subscription")
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

    @Test
    public void shouldReturnConsumerGroupDescription() {
        // given
        String subscriptionName = "subscription";
        Topic topic = operations.buildTopic(randomTopic("topicGroup", "test").build());
        operations.createSubscription(topic, subscriptionName, remoteService.getUrl());

        TestMessage message = TestMessage.of("hello", "world");
        remoteService.expectMessages(message.body());
        publisher.publish(topic.getQualifiedName(), message.body());
        remoteService.waitUntilReceived();

        // when
        List<ConsumerGroup> response = management.subscription().describeConsumerGroups(
                topic.getQualifiedName(), subscriptionName);

        // then
        wait.until(() -> {
            assertThat(response.size()).isGreaterThan(0);
            assertThat(response)
                    .flatExtracting("members")
                    .flatExtracting("partitions")
                    .usingRecursiveFieldByFieldElementComparatorIgnoringFields("partition", "topic", "offsetMetadata", "contentType")
                    .containsOnlyOnce(new TopicPartition(-1, "any", 0, 1, "any", topic.getContentType()));
        });
    }

    @Test
    public void shouldNotCreateSubscriptionNotOwnedByTheUser() {
        // given
        Topic topic = createTopic();
        Subscription subscription = subscription(topic.getQualifiedName(), "subscription")
                .withOwner(new OwnerId("Plaintext", "subscriptionOwner"))
                .build();
        TestSecurityProvider.setUserIsAdmin(false);

        // when
        Response response = management.subscription().create(topic.getQualifiedName(), subscription);

        // then
        assertThat(response)
                .hasStatus(BAD_REQUEST)
                .containsMessage("Provide an owner that includes you, you would not be able to manage this subscription later");
    }

    @Test
    public void shouldNotUpdateSubscriptionNotOwnedByTheUser() {
        // given
        Topic topic = createTopic();
        Subscription subscription = operations.createSubscription(topic, "subscription", "http://localhost:8081/topics/test-topic");
        TestSecurityProvider.setUserIsAdmin(false);

        // when
        Response response = management.subscription().update(
                topic.getQualifiedName(),
                subscription.getName(),
                patchData().set("endpoint", EndpointAddress.of("http://localhost:7777/topics/test-topic")).build()
        );

        // then
        assertThat(response)
                .hasStatus(BAD_REQUEST)
                .containsMessage("Provide an owner that includes you, you would not be able to manage this subscription later");
    }

    @Test
    public void shouldAllowAdminToBypassSubscribingRestrictions() {
        // given
        Topic topic = createTopicWithSubscribingRestricted();
        Subscription subscription = subscription(topic.getQualifiedName(), "subscription")
                .withEndpoint("http://localhost:8081/topics/test-topic")
                .build();
        TestSecurityProvider.setUserIsAdmin(true);

        // when
        Response createResponse = management.subscription().create(topic.getQualifiedName(), subscription);

        // then
        assertThat(createResponse).hasStatus(CREATED);

        // when
        Response updateResponse = management.subscription().update(
                topic.getQualifiedName(),
                subscription.getName(),
                patchData().set("endpoint", EndpointAddress.of("http://localhost:7777/topics/test-topic")).build()
        );

        // then
        assertThat(updateResponse).hasStatus(OK);
    }

    @Test
    public void shouldAllowTopicOwnerToBypassSubscribingRestrictions() {
        // given
        Topic topic = createTopicWithSubscribingRestricted();
        Subscription subscription = subscription(topic.getQualifiedName(), "subscription")
                .withEndpoint("http://localhost:8081/topics/test-topic")
                .build();
        TestSecurityProvider.setUserIsAdmin(false);
        TestSecurityProvider.setUserAsOwner(topic.getOwner(), subscription.getOwner());

        // when
        Response response = management.subscription().create(topic.getQualifiedName(), subscription);

        // then
        assertThat(response).hasStatus(CREATED);

        // when
        Response updateResponse = management.subscription().update(
                topic.getQualifiedName(),
                subscription.getName(),
                patchData().set("endpoint", EndpointAddress.of("http://localhost:7777/topics/test-topic")).build()
        );

        // then
        assertThat(updateResponse).hasStatus(OK);
    }

    @Test
    public void shouldAllowPrivilegedSubscriberToBypassSubscribingRestrictions() {
        // given
        Topic topic = createTopicWithSubscribingRestricted();
        Subscription subscription = subscription(topic.getQualifiedName(), "subscription")
                .withOwner(new OwnerId("Plaintext", "subscriberAllowedToAccessAnyTopic"))
                .withEndpoint("http://localhost:8081/topics/test-topic")
                .build();
        TestSecurityProvider.setUserIsAdmin(false);
        TestSecurityProvider.setUserAsOwner(subscription.getOwner());

        // when
        Response response = management.subscription().create(topic.getQualifiedName(), subscription);

        // then
        assertThat(response).hasStatus(CREATED);

        // when
        Response updateResponse = management.subscription().update(
                topic.getQualifiedName(),
                subscription.getName(),
                patchData().set("endpoint", EndpointAddress.of("http://localhost:7777/topics/test-topic")).build()
        );

        // then
        assertThat(updateResponse).hasStatus(OK);
    }

    @Test
    public void shouldRespectPrivilegedSubscriberProtocolsWhileCreatingSubscription() {
        // given
        Topic topic = createTopicWithSubscribingRestricted();
        Subscription subscription = subscription(topic.getQualifiedName(), "subscription")
                .withOwner(new OwnerId("Plaintext", "subscriberAllowedToAccessAnyTopic"))
                .withEndpoint("googlepubsub://pubsub.googleapis.com:443/projects/test-project/topics/test-topic")
                .build();
        TestSecurityProvider.setUserIsAdmin(false);
        TestSecurityProvider.setUserAsOwner(subscription.getOwner());

        // when
        Response response = management.subscription().create(topic.getQualifiedName(), subscription);

        // then
        assertThat(response)
                .hasStatus(FORBIDDEN)
                .containsMessage("Subscribing to this topic has been restricted. Contact the topic owner to create a new subscription.");
    }

    @Test
    @SuppressWarnings("checkstyle:LineLengthCheck")
    public void shouldRespectPrivilegedSubscriberProtocolsWhileUpdatingEndpoint() {
        // given
        Topic topic = createTopicWithSubscribingRestricted();
        Subscription subscription = subscription(topic.getQualifiedName(), "subscription")
                .withOwner(new OwnerId("Plaintext", "subscriberAllowedToAccessAnyTopic"))
                .withEndpoint("http://localhost:8081/topics/test-topic")
                .build();
        operations.createSubscription(topic, subscription);
        TestSecurityProvider.setUserIsAdmin(false);
        TestSecurityProvider.setUserAsOwner(subscription.getOwner());

        // when
        Response response = management.subscription().update(
                topic.getQualifiedName(),
                subscription.getName(),
                patchData().set(
                        "endpoint", EndpointAddress.of("googlepubsub://pubsub.googleapis.com:443/projects/test-project/topics/test-topic")
                ).build()
        );

        // then
        assertThat(response)
                .hasStatus(FORBIDDEN)
                .containsMessage(
                        "Subscribing to this topic has been restricted. Contact the topic owner to modify the endpoint of this subscription."
                );
    }

    @Test
    public void shouldNotAllowUnprivilegedUserToCreateSubscriptionWhenSubscribingIsRestricted() {
        // given
        Topic topic = createTopicWithSubscribingRestricted();
        Subscription subscription = subscription(topic.getQualifiedName(), "subscription")
                .withOwner(new OwnerId("Plaintext", "subscriptionOwner"))
                .build();
        TestSecurityProvider.setUserIsAdmin(false);
        TestSecurityProvider.setUserAsOwner(subscription.getOwner());

        // when
        Response response = management.subscription().create(topic.getQualifiedName(), subscription);

        // then
        assertThat(response)
                .hasStatus(FORBIDDEN)
                .containsMessage("Subscribing to this topic has been restricted. Contact the topic owner to create a new subscription.");
    }

    @Test
    @SuppressWarnings("checkstyle:LineLengthCheck")
    public void shouldNotAllowUnprivilegedUserToUpdateEndpointWhenSubscribingIsRestricted() {
        // given
        Topic topic = createTopicWithSubscribingRestricted();
        Subscription subscription = subscription(topic.getQualifiedName(), "subscription")
                .withOwner(new OwnerId("Plaintext", "subscriptionOwner"))
                .withEndpoint("http://localhost:8081/topics/test-topic")
                .build();
        operations.createSubscription(topic, subscription);
        TestSecurityProvider.setUserIsAdmin(false);
        TestSecurityProvider.setUserAsOwner(subscription.getOwner());

        // when
        Response response = management.subscription().update(
                topic.getQualifiedName(),
                subscription.getName(),
                patchData().set("endpoint", EndpointAddress.of("http://localhost:7777/topics/test-topic")).build()
        );

        // then
        assertThat(response)
                .hasStatus(FORBIDDEN)
                .containsMessage(
                        "Subscribing to this topic has been restricted. Contact the topic owner to modify the endpoint of this subscription."
                );
    }

    @Test
    public void shouldSetInflightSizeToNullByDefault() {
        // given
        Topic topic = createTopic();
        Subscription subscription = operations.createSubscription(topic, "subscription", "http://localhost:8081/topics/test-topic");
        TestSecurityProvider.setUserIsAdmin(false);

        // when
        Subscription response = management.subscription().get(
                topic.getQualifiedName(),
                subscription.getName()
        );

        // then
        assertThat(response.getSerialSubscriptionPolicy().getInflightSize()).isNull();
    }

    @Test
    public void shouldReturnInflightSizeWhenSetToNonNullValue() {
        // given
        Topic topic = createTopic();
        Subscription subscription = operations.createSubscription(topic,
                subscriptionWithInflight(topic, "subscription", 42)
        );
        TestSecurityProvider.setUserIsAdmin(false);

        // when
        Subscription response = management.subscription().get(
                topic.getQualifiedName(),
                subscription.getName()
        );

        // then
        assertThat(response.getSerialSubscriptionPolicy().getInflightSize()).isEqualTo(42);
    }

    @Test
    public void shouldNotAllowNonAdminUserToSetInflightSize() {
        // given
        Topic topic = createTopic();
        Subscription subscription = operations.createSubscription(topic, "subscription", "http://localhost:8081/topics/test-topic");
        TestSecurityProvider.setUserIsAdmin(false);

        PatchData patchData = patchData().set("subscriptionPolicy", ImmutableMap.builder()
                .put("inflightSize", 100)
                .build()
        ).build();

        // when
        Response response = management.subscription().update(
                topic.getQualifiedName(),
                subscription.getName(),
                patchData
        );

        //then
        assertThat(response).hasStatus(BAD_REQUEST).containsMessage("Subscription.serialSubscriptionPolicy.inflightSize must be null");
    }

    @Test
    public void shouldAllowAdminUserToSetInflightSize() {
        // given
        Topic topic = createTopic();
        Subscription subscription = operations.createSubscription(topic, "subscription", "http://localhost:8081/topics/test-topic");
        TestSecurityProvider.setUserIsAdmin(true);

        PatchData patchData = patchData().set("subscriptionPolicy", ImmutableMap.builder()
                .put("inflightSize", 100)
                .build()
        ).build();

        // when
        Response response = management.subscription().update(
                topic.getQualifiedName(),
                subscription.getName(),
                patchData
        );

        //then
        assertThat(response).hasStatus(OK);
    }


    private Topic createTopic() {
        return operations.buildTopic(
                randomTopic("group", "topic")
                        .withOwner(new OwnerId("Plaintext", "topicOwner"))
                        .build()
        );
    }

    private Topic createTopicWithSubscribingRestricted() {
        return operations.buildTopic(
                randomTopic("restrictedGroup", "topic")
                        .withSubscribingRestricted()
                        .withOwner(new OwnerId("Plaintext", "topicOwner"))
                        .build()
        );
    }

    private static Subscription subscriptionWithInflight(Topic topic, String subscriptionName,  Integer inflightSize) {
        return subscription(topic, subscriptionName)
                .withSubscriptionPolicy(
                        SubscriptionPolicy.Builder.subscriptionPolicy()
                                .withInflightSize(inflightSize)
                                .build()
                ).build();
    }

    private List<Map<String, String>> getMessageTrace(String topic, String subscription, String messageId) {
        Response response = management.subscription().getMessageTrace(topic, subscription, messageId);
        return response.readEntity(new GenericType<>() {
        });
    }

    private String publishMessage(String topic, String body) {
        return client.publish(topic, body).join().getMessageId();
    }
}
