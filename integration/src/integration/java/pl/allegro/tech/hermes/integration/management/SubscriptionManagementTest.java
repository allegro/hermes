package pl.allegro.tech.hermes.integration.management;

import com.jayway.awaitility.Duration;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.SubscriptionHealth;
import pl.allegro.tech.hermes.api.SubscriptionPolicy;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.client.HermesClient;
import pl.allegro.tech.hermes.client.jersey.JerseyHermesSender;
import pl.allegro.tech.hermes.integration.IntegrationTest;
import pl.allegro.tech.hermes.integration.env.SharedServices;
import pl.allegro.tech.hermes.integration.helper.PrometheusEndpoint;
import pl.allegro.tech.hermes.integration.helper.PrometheusEndpoint.PrometheusTopicResponse;
import pl.allegro.tech.hermes.management.TestSecurityProvider;
import pl.allegro.tech.hermes.test.helper.endpoint.BrokerOperations.ConsumerGroupOffset;
import pl.allegro.tech.hermes.test.helper.endpoint.RemoteServiceEndpoint;
import pl.allegro.tech.hermes.test.helper.message.TestMessage;

import java.util.List;
import java.util.Map;

import static jakarta.ws.rs.client.ClientBuilder.newClient;
import static java.net.URI.create;
import static pl.allegro.tech.hermes.api.SubscriptionHealth.Status.UNHEALTHY;
import static pl.allegro.tech.hermes.api.SubscriptionHealthProblem.malfunctioning;
import static pl.allegro.tech.hermes.client.HermesClientBuilder.hermesClient;
import static pl.allegro.tech.hermes.integration.helper.PrometheusEndpoint.PrometheusSubscriptionResponseBuilder.builder;
import static pl.allegro.tech.hermes.integration.test.HermesAssertions.assertThat;
import static pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder.subscription;
import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.randomTopic;

public class SubscriptionManagementTest extends IntegrationTest {

    public static final TestMessage MESSAGE = TestMessage.of("hello", "world");

    private RemoteServiceEndpoint remoteService;
    private HermesClient client;
    private PrometheusEndpoint prometheusEndpoint;

    @BeforeMethod
    public void initializeAlways() {
        remoteService = new RemoteServiceEndpoint(SharedServices.services().serviceMock());
        client = hermesClient(new JerseyHermesSender(newClient())).withURI(create("http://localhost:" + FRONTEND_PORT)).build();
        prometheusEndpoint = new PrometheusEndpoint(SharedServices.services().prometheusHttpMock());
    }

    @AfterMethod
    public void cleanup() {
        TestSecurityProvider.reset();
    }

    @Test
    public void shouldMoveOffsetsToTheEnd() {
        // given
        Topic topic = operations.buildTopic(randomTopic("moveSubscriptionOffsets", "topic").build());
        Subscription subscription = subscription(topic.getQualifiedName(), "subscription", remoteService.getUrl())
                // prevents discarding messages and moving offsets
                .withSubscriptionPolicy(SubscriptionPolicy.create(Map.of("messageTtl", 3600)))
                .build();
        management.subscription().create(subscription.getQualifiedTopicName(), subscription);
        List<String> messages = List.of(MESSAGE.body(), MESSAGE.body(), MESSAGE.body(), MESSAGE.body());

        // prevents from moving offsets during messages sending
        remoteService.setReturnedStatusCode(503);
        remoteService.expectMessages(messages);
        publishMessages(topic.getQualifiedName(), messages);
        remoteService.waitUntilReceived();

        assertThat(allConsumerGroupOffsetsMovedToTheEnd(subscription)).isFalse();

        management.subscription().remove(topic.getQualifiedName(), subscription.getName());

        // when
        wait.awaitAtMost(Duration.TEN_SECONDS)
                .until(() -> management
                        .subscription()
                        .moveOffsetsToTheEnd(topic.getQualifiedName(), subscription.getName()).getStatus() == 200);

        // then
        assertThat(allConsumerGroupOffsetsMovedToTheEnd(subscription)).isTrue();
    }

    @Test
    public void shouldReturnHealthyStatusForAHealthySubscription() {
        // given
        Topic topic = randomTopic("healthHealthy", "topic").build();
        String subscriptionName = "subscription";

        // and
        operations.buildTopic(topic);
        operations.createSubscription(topic, subscriptionName, remoteService.getUrl());
        prometheusEndpoint.returnTopicMetrics(topic, new PrometheusTopicResponse(100, 100, 0));
        prometheusEndpoint.returnSubscriptionMetrics(topic, subscriptionName, builder().withRate(100).build());

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
        prometheusEndpoint.returnTopicMetrics(topic, new PrometheusTopicResponse(100, 50, 0));
        prometheusEndpoint.returnSubscriptionMetrics(topic, "subscription",
                builder().withRate(50).withRatedStatusCode("500", 11).build());

        // when
        SubscriptionHealth subscriptionHealth = management.subscription().getHealth(topic.getQualifiedName(), subscriptionName);

        // then
        assertThat(subscriptionHealth.getStatus()).isEqualTo(UNHEALTHY);
        assertThat(subscriptionHealth.getProblems()).containsOnly(malfunctioning(11, topic.getQualifiedName() + "$subscription"));
    }

    @Test
    public void shouldReturnNoDataStatusWhenPrometheusRespondsWithAnError() {
        // given
        Topic topic = randomTopic("healthNoData", "topic").build();
        String subscriptionName = "subscription";

        // and
        operations.buildTopic(topic);
        operations.createSubscription(topic, subscriptionName, remoteService.getUrl());
        prometheusEndpoint.returnServerErrorForAllTopics();
        prometheusEndpoint.returnSubscriptionMetrics(topic, "subscription",
                builder().withRate(100).build());

        // when
        SubscriptionHealth subscriptionHealth = management.subscription().getHealth(topic.getQualifiedName(), subscriptionName);

        // then
        assertThat(subscriptionHealth).isEqualTo(SubscriptionHealth.NO_DATA);
    }

    private void publishMessages(String topic, List<String> messages) {
        messages.forEach(it -> publishMessage(topic, it));
    }

    private String publishMessage(String topic, String body) {
        return client.publish(topic, body).join().getMessageId();
    }

    private boolean allConsumerGroupOffsetsMovedToTheEnd(Subscription subscription) {
        List<ConsumerGroupOffset> partitionsOffsets = brokerOperations.getTopicPartitionsOffsets(subscription.getQualifiedName());
        return !partitionsOffsets.isEmpty() && partitionsOffsets.stream().allMatch(ConsumerGroupOffset::movedToEnd);
    }
}
