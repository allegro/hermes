package pl.allegro.tech.hermes.integration;

import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import org.assertj.core.groups.Tuple;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import pl.allegro.tech.hermes.api.ContentType;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.SubscriptionPolicy;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.integration.env.SharedServices;
import pl.allegro.tech.hermes.test.helper.avro.AvroUser;
import pl.allegro.tech.hermes.test.helper.avro.AvroUserSchemaLoader;
import pl.allegro.tech.hermes.test.helper.endpoint.RemoteServiceEndpoint;
import pl.allegro.tech.hermes.test.helper.message.TestMessage;

import static com.google.common.collect.ImmutableMap.of;
import static javax.ws.rs.core.Response.Status.CREATED;
import static pl.allegro.tech.hermes.api.ContentType.AVRO;
import static pl.allegro.tech.hermes.integration.test.HermesAssertions.assertThat;
import static pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder.subscription;
import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.topic;
import static pl.allegro.tech.hermes.test.helper.message.TestMessage.random;

public class FilteringTest extends IntegrationTest {

    private RemoteServiceEndpoint remoteService;

    private final static String MESSAGE_REGEX_FILTER =
            "{ \n" +
                    "  \"type\": \"avpath\",\n" +
                    "  \"expression\": {\n" +
                    "    \"path\": \".name\",\n" +
                    "    \"matcher\": \"Bob.*\"\n" +
                    "  }\n" +
                    "}";

//    private final static String HEADER_REGEX_FILTER =
//            "{ \n" +
//                    "  \"type\": \"http_header\",\n" +
//                    "  \"expression\": {\n" +
//                    "    \"name\": \"Foo\",\n" +
//                    "    \"match_regex\": \".*bar.*\"\n" +
//                    "  }\n" +
//                    "}";

    final static AvroUser BOB = new AvroUser("Bob", 50, "blue");
    final static AvroUser ALICE = new AvroUser("Alice", 20, "magenta");

    @BeforeMethod
    public void initializeAlways() {
        this.remoteService = new RemoteServiceEndpoint(SharedServices.services().serviceMock());
    }

    @Test
    public void shouldFilterIncomingEvents() {
        // given
        final Topic topic = topic("filteredTopic.topic")
                .withValidation(true)
                .withMessageSchema(AvroUserSchemaLoader.load().toString())
                .withContentType(AVRO).build();
        operations.buildTopic(topic);

        final Subscription subscription = subscription(topic.getName(), "subscription")
                .withEndpoint(HTTP_ENDPOINT_URL)
                .withContentType(ContentType.JSON)
                .withSupportTeam("team")
                .withSubscriptionPolicy(new SubscriptionPolicy(100, 10, false, 100))
                .withFilter(MESSAGE_REGEX_FILTER)
                .build();

        operations.createSubscription(topic, subscription);

        remoteService.expectMessages(BOB.asJson());

        // when
        assertThat(publisher.publish(topic.getQualifiedName(), ALICE.asJson())).hasStatus(CREATED);
        assertThat(publisher.publish(topic.getQualifiedName(), BOB.asJson())).hasStatus(CREATED);

        // then
        remoteService.waitUntilReceived();
        assertThat(remoteService.getReceivedRequests())
                .extracting(LoggedRequest::getBodyAsString)
                .containsOnly(BOB.asJson());
    }

//    @Test
//    public void shouldChainMultipleFilters() {
//        // given
//        Topic topic = operations.buildTopic("filteredTopic", "topic");
//        final Subscription subscription = subscription(topic.getName(), "subscription")
//                .withEndpoint(HTTP_ENDPOINT_URL)
//                .withContentType(ContentType.AVRO)
//                .withSubscriptionPolicy(new SubscriptionPolicy(100, 10, false, 100))
//                .withFilter(MESSAGE_REGEX_FILTER)
//                .withFilter(HEADER_REGEX_FILTER)
//                .build();
//
//        operations.createSubscription(topic, subscription);
//        final TestMessage message = TestMessage.of("message", "ok");
//
//        // when
//        assertThat(publisher.publish(topic.getQualifiedName(), random().body())).hasStatus(CREATED);
//        assertThat(publisher.publish(topic.getQualifiedName(), message.body())).hasStatus(CREATED);
//        assertThat(publisher.publish(topic.getQualifiedName(), message.body(), of("Foo", "xxx-bar-yyy"))).hasStatus(CREATED);
//
//        // then
//        remoteService.waitUntilRequestReceived(req -> req.containsHeader("Foo"));
//        assertThat(remoteService.getReceivedRequests())
//                .extracting(lr -> new Tuple(lr.getHeader("Foo"), lr.getBody()))
//                .containsOnly(new Tuple("xxx-bar-yyy", message.body()));
//
//    }

}
