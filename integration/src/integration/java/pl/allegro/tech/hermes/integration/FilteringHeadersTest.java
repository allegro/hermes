package pl.allegro.tech.hermes.integration;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import pl.allegro.tech.hermes.api.ContentType;
import pl.allegro.tech.hermes.api.MessageFilterSpecification;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.integration.env.SharedServices;
import pl.allegro.tech.hermes.test.helper.avro.AvroUser;
import pl.allegro.tech.hermes.test.helper.endpoint.RemoteServiceEndpoint;

import static com.google.common.collect.ImmutableMap.of;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static javax.ws.rs.core.Response.Status.CREATED;
import static pl.allegro.tech.hermes.integration.test.HermesAssertions.assertThat;
import static pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder.subscription;
import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.randomTopic;

public class FilteringHeadersTest extends IntegrationTest {

    private RemoteServiceEndpoint remoteService;

    private static final MessageFilterSpecification TRACE_ID_HEADER_FILTER =
            new MessageFilterSpecification(of("type", "header", "header", "Trace-Id", "matcher", "^vte.*"));

    private static final MessageFilterSpecification SPAN_ID_HEADER_FILTER =
            new MessageFilterSpecification(of("type", "header", "header", "Span-Id", "matcher", ".*span$"));

    private static final AvroUser ALICE = new AvroUser("Alice", 20, "blue");
    private static final AvroUser BOB = new AvroUser("Bob", 30, "red");

    @BeforeMethod
    public void initializeAlways() {
        this.remoteService = new RemoteServiceEndpoint(SharedServices.services().serviceMock());
    }

    @Test
    public void shouldFilterIncomingEventsByHeaders() {
        // given
        Topic topic = randomTopic("filteredHeaders", "topic").build();
        operations.buildTopic(topic);

        Subscription subscription = subscription(topic.getName(), "subscription")
                .withEndpoint(remoteService.getUrl())
                .withContentType(ContentType.JSON)
                .withFilter(TRACE_ID_HEADER_FILTER)
                .withFilter(SPAN_ID_HEADER_FILTER)
                .build();

        operations.createSubscription(topic, subscription);

        remoteService.expectMessages(ALICE.asJson());

        // when
        assertThat(publisher.publish(
                topic.getQualifiedName(), ALICE.asJson(), of("Trace-Id", "vte12", "Span-Id", "my-span", "Content-Type", TEXT_PLAIN))
        ).hasStatus(CREATED);

        assertThat(publisher.publish(topic.getQualifiedName(), BOB.asJson(), of("Trace-Id", "vte12"))).hasStatus(CREATED);
        assertThat(publisher.publish(topic.getQualifiedName(), BOB.asJson(), of("Span-Id", "my-span"))).hasStatus(CREATED);
        assertThat(publisher.publish(topic.getQualifiedName(), BOB.asJson(), of("Trace-Id", "vte12", "Span-Id", "span-1")))
                .hasStatus(CREATED);
        assertThat(publisher.publish(topic.getQualifiedName(), BOB.asJson(), of("Trace-Id", "invalid", "Span-Id", "my-span")))
                .hasStatus(CREATED);

        // then
        remoteService.waitUntilReceived();
    }
}
