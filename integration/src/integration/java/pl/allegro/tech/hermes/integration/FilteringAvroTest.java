package pl.allegro.tech.hermes.integration;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import pl.allegro.tech.hermes.api.ContentType;
import pl.allegro.tech.hermes.api.MessageFilterSpecification;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.integration.env.SharedServices;
import pl.allegro.tech.hermes.test.helper.avro.AvroUser;
import pl.allegro.tech.hermes.test.helper.avro.AvroUserSchemaLoader;
import pl.allegro.tech.hermes.test.helper.endpoint.RemoteServiceEndpoint;

import static com.google.common.collect.ImmutableMap.of;
import static javax.ws.rs.core.Response.Status.CREATED;
import static pl.allegro.tech.hermes.api.ContentType.AVRO;
import static pl.allegro.tech.hermes.api.TopicWithSchema.topicWithSchema;
import static pl.allegro.tech.hermes.integration.test.HermesAssertions.assertThat;
import static pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder.subscription;
import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.randomTopic;

public class FilteringAvroTest extends IntegrationTest {

    private RemoteServiceEndpoint remoteService;

    private static final MessageFilterSpecification MESSAGE_NAME_FILTER =
            new MessageFilterSpecification(of("type", "avropath", "path", ".name", "matcher", "Bob"));

    private static final MessageFilterSpecification MESSAGE_COLOR_FILTER =
            new MessageFilterSpecification(of("type", "avropath", "path", ".favoriteColor", "matcher", "grey"));


    private static final AvroUser BOB = new AvroUser("Bob", 50, "blue");
    private static final AvroUser ALICE = new AvroUser("Alice", 20, "magenta");
    private static final AvroUser BOB_GREY = new AvroUser("Bob", 50, "grey");

    @BeforeMethod
    public void initializeAlways() {
        this.remoteService = new RemoteServiceEndpoint(SharedServices.services().serviceMock());
    }

    @Test
    public void shouldFilterIncomingEvents() {
        // given
        final String schema = AvroUserSchemaLoader.load().toString();
        final Topic topic = randomTopic("filteredTopic", "topic")
                .withContentType(AVRO).build();
        operations.buildTopicWithSchema(topicWithSchema(topic, schema));

        final Subscription subscription = subscription(topic.getName(), "subscription")
                .withEndpoint(remoteService.getUrl())
                .withContentType(ContentType.JSON)
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
    public void shouldChainMultipleFilters() {
        // given
        final String schema = AvroUserSchemaLoader.load().toString();
        final Topic topic = randomTopic("filteredChainTopic", "topic")
                .withContentType(AVRO).build();
        operations.buildTopicWithSchema(topicWithSchema(topic, schema));

        final Subscription subscription = subscription(topic.getName(), "subscription")
                .withEndpoint(remoteService.getUrl())
                .withContentType(ContentType.JSON)
                .withFilter(MESSAGE_NAME_FILTER)
                .withFilter(MESSAGE_COLOR_FILTER)
                .build();

        operations.createSubscription(topic, subscription);
        remoteService.expectMessages(BOB_GREY.asJson());

        // when
        assertThat(publisher.publish(topic.getQualifiedName(), BOB.asJson())).hasStatus(CREATED);
        assertThat(publisher.publish(topic.getQualifiedName(), BOB_GREY.asJson())).hasStatus(CREATED);

        // then
        remoteService.waitUntilReceived();
    }
}
