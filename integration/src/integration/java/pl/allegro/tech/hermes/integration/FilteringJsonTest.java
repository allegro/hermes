package pl.allegro.tech.hermes.integration;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import pl.allegro.tech.hermes.api.ContentType;
import pl.allegro.tech.hermes.api.MessageFilterSpecification;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.SubscriptionPolicy;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.integration.env.SharedServices;
import pl.allegro.tech.hermes.test.helper.avro.AvroUser;
import pl.allegro.tech.hermes.test.helper.endpoint.RemoteServiceEndpoint;

import static com.google.common.collect.ImmutableMap.of;
import static javax.ws.rs.core.Response.Status.CREATED;
import static pl.allegro.tech.hermes.integration.test.HermesAssertions.assertThat;
import static pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder.subscription;
import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.randomTopic;

public class FilteringJsonTest extends IntegrationTest {
    private RemoteServiceEndpoint remoteService;

    private static final MessageFilterSpecification MESSAGE_NAME_FILTER =
            new MessageFilterSpecification(of("type", "jsonpath", "path", ".name", "matcher", "^Bob.*"));

    private static final MessageFilterSpecification MESSAGE_COLOR_FILTER =
            new MessageFilterSpecification(of("type", "jsonpath", "path", ".favoriteColor", "matcher", "grey"));


    static final AvroUser BOB = new AvroUser("Bob", 50, "blue");
    static final AvroUser ALICE = new AvroUser("Alice", 20, "magenta");
    static final AvroUser ALICE_GREY = new AvroUser("Alice", 20, "grey");
    static final AvroUser BOB_GREY = new AvroUser("Bob", 50, "grey");

    private static final SubscriptionPolicy SUBSCRIPTION_POLICY = new SubscriptionPolicy(100, 2000, 1000, 1000, true, 100, null, 0, 1, 600);

    @BeforeMethod
    public void initializeAlways() {
        this.remoteService = new RemoteServiceEndpoint(SharedServices.services().serviceMock());
    }

    @Test
    public void shouldFilterIncomingEvents() {
        // given
        Topic topic = operations.buildTopic(randomTopic("filteredTopicJson", "topic").build());
        final Subscription subscription = subscription(topic.getName(), "subscription")
                .withEndpoint(remoteService.getUrl())
                .withContentType(ContentType.JSON)
                .withSubscriptionPolicy(SUBSCRIPTION_POLICY)
                .withFilter(MESSAGE_NAME_FILTER)
                .build();

        operations.createSubscription(topic, subscription);
        remoteService.expectMessages(BOB.asJson());

        // when
        assertThat(publisher.publish(topic.getQualifiedName(), ALICE.asJson())).hasStatus(CREATED);
        assertThat(publisher.publish(topic.getQualifiedName(), BOB.asJson())).hasStatus(CREATED);

        // then
        remoteService.waitUntilReceived();
    }

    @Test
    public void shouldChainFilters() {
        // given
        Topic topic = operations.buildTopic(randomTopic("filteredChainedTopicJson", "topic").build());
        final Subscription subscription = subscription(topic.getName(), "subscription")
                .withEndpoint(remoteService.getUrl())
                .withContentType(ContentType.JSON)
                .withSubscriptionPolicy(SUBSCRIPTION_POLICY)
                .withFilter(MESSAGE_NAME_FILTER)
                .withFilter(MESSAGE_COLOR_FILTER)
                .build();

        operations.createSubscription(topic, subscription);
        remoteService.expectMessages(BOB_GREY.asJson());

        // when
        assertThat(publisher.publish(topic.getQualifiedName(), ALICE.asJson())).hasStatus(CREATED);
        assertThat(publisher.publish(topic.getQualifiedName(), ALICE_GREY.asJson())).hasStatus(CREATED);
        assertThat(publisher.publish(topic.getQualifiedName(), BOB.asJson())).hasStatus(CREATED);
        assertThat(publisher.publish(topic.getQualifiedName(), BOB_GREY.asJson())).hasStatus(CREATED);

        // then
        remoteService.waitUntilReceived();
    }

    @Test
    public void shouldPassSubscriptionHeadersWhenFilteringIsEnabledForIncomingEvents() {
        // given
        Topic topic = operations.buildTopic(randomTopic("filteredJsonTopicHavingSubscriptionWithHeaders", "topic").build());
        final Subscription subscription = subscription(topic.getName(), "subscription")
                .withEndpoint(remoteService.getUrl())
                .withContentType(ContentType.JSON)
                .withSubscriptionPolicy(SUBSCRIPTION_POLICY)
                .withFilter(MESSAGE_NAME_FILTER)
                .withHeader("MY-HEADER", "myHeaderValue")
                .build();

        operations.createSubscription(topic, subscription);
        remoteService.expectMessages(BOB.asJson());

        // when
        assertThat(publisher.publish(topic.getQualifiedName(), ALICE.asJson())).hasStatus(CREATED);
        assertThat(publisher.publish(topic.getQualifiedName(), BOB.asJson())).hasStatus(CREATED);

        // then
        remoteService.waitUntilReceived();
        remoteService.waitUntilRequestReceived(request -> assertThat(request.getHeader("MY-HEADER")).isEqualTo("myHeaderValue"));
    }
}
