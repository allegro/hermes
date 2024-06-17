package pl.allegro.tech.hermes.integrationtests;

import net.javacrumbs.jsonunit.core.Option;
import org.apache.avro.Schema;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.web.reactive.server.WebTestClient;
import pl.allegro.tech.hermes.api.ContentType;
import pl.allegro.tech.hermes.api.PatchData;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.api.TopicWithSchema;
import pl.allegro.tech.hermes.client.HermesMessage;
import pl.allegro.tech.hermes.integrationtests.setup.HermesExtension;
import pl.allegro.tech.hermes.integrationtests.subscriber.TestSubscriber;
import pl.allegro.tech.hermes.integrationtests.subscriber.TestSubscribersExtension;
import pl.allegro.tech.hermes.schema.CompiledSchema;
import pl.allegro.tech.hermes.test.helper.avro.AvroUser;
import pl.allegro.tech.hermes.test.helper.avro.AvroUserSchemaLoader;
import pl.allegro.tech.hermes.test.helper.message.TestMessage;

import java.time.Clock;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import static jakarta.ws.rs.core.MediaType.TEXT_PLAIN;
import static java.util.Collections.singletonMap;
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.waitAtMost;
import static pl.allegro.tech.hermes.api.AvroMediaType.AVRO_JSON;
import static pl.allegro.tech.hermes.api.ContentType.AVRO;
import static pl.allegro.tech.hermes.api.ContentType.JSON;
import static pl.allegro.tech.hermes.api.PatchData.patchData;
import static pl.allegro.tech.hermes.api.TopicWithSchema.topicWithSchema;
import static pl.allegro.tech.hermes.client.HermesMessage.hermesMessage;
import static pl.allegro.tech.hermes.consumers.supervisor.process.Signal.SignalType.COMMIT;
import static pl.allegro.tech.hermes.consumers.supervisor.process.Signal.SignalType.UPDATE_SUBSCRIPTION;
import static pl.allegro.tech.hermes.test.helper.avro.AvroUserSchemaLoader.load;
import static pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder.subscription;
import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.topicWithRandomName;
import static pl.allegro.tech.hermes.test.helper.endpoint.TimeoutAdjuster.adjust;
import static pl.allegro.tech.hermes.utils.Headers.createHeaders;

public class PublishingAvroTest {

    private static final Logger logger = LoggerFactory.getLogger(PublishingAvroTest.class);

    private final Clock clock = Clock.systemDefaultZone();

    @RegisterExtension
    public static final HermesExtension hermes = new HermesExtension();

    @RegisterExtension
    public static final TestSubscribersExtension subscribers = new TestSubscribersExtension();

    private static final AvroUser user = new AvroUser("Bob", 50, "blue");


    @Test
    public void shouldPublishAvroAndConsumeJsonMessage() {
        // given
        TopicWithSchema topicWithSchema = topicWithSchema(topicWithRandomName()
                        .withContentType(AVRO)
                        .build(), user.getSchemaAsString());
        Topic topic = hermes.initHelper().createTopicWithSchema(topicWithSchema);

        TestSubscriber subscriber = subscribers.createSubscriber();

        hermes.initHelper().createSubscription(
                subscription(topic.getQualifiedName(), "subscription", subscriber.getEndpoint()).build()
        );

        // when
        hermes.api().publishAvroUntilSuccess(topic.getQualifiedName(), user.asBytes());

        // then
        subscriber.waitUntilReceived(user.asJson());
    }

    @Test
    public void shouldNotPublishAvroWhenMessageIsNotJsonOrAvro() {
        // given
        TopicWithSchema topicWithSchema = topicWithSchema(topicWithRandomName()
                .withContentType(AVRO)
                .build(), user.getSchemaAsString());
        Topic topic = hermes.initHelper().createTopicWithSchema(topicWithSchema);

        TestSubscriber subscriber = subscribers.createSubscriber();

        hermes.initHelper().createSubscription(
                subscription(topic.getQualifiedName(), "subscription", subscriber.getEndpoint()).build()
        );

        // when
        WebTestClient.ResponseSpec response = hermes.api().publish(topic.getQualifiedName(), user.asJson(), createHeaders(Map.of("Content-Type", TEXT_PLAIN)));

        // then
        response.expectStatus().isBadRequest();
    }

    @Test
    public void shouldPublishAvroAndConsumeAvroMessage() {
        // given
        TopicWithSchema topicWithSchema = topicWithSchema(topicWithRandomName()
                .withContentType(AVRO)
                .build(), user.getSchemaAsString());
        Topic topic = hermes.initHelper().createTopicWithSchema(topicWithSchema);

        TestSubscriber subscriber = subscribers.createSubscriber();

        hermes.initHelper().createSubscription(
                subscription(topic.getQualifiedName(), "subscription", subscriber.getEndpoint())
                        .withContentType(AVRO)
                        .build()
        );

        // when
        hermes.api().publishAvroUntilSuccess(topic.getQualifiedName(), user.asBytes());

        // then
        subscriber.waitUntilRequestReceived(request -> assertBodyDeserializesIntoUser(request.getBodyAsString(), user));
    }

    @Test
    public void shouldSendAvroAfterSubscriptionContentTypeChanged() {
        // given avro topic
        TopicWithSchema topicWithSchema = topicWithSchema(topicWithRandomName()
                .withContentType(AVRO)
                .build(), user.getSchemaAsString());
        Topic topic = hermes.initHelper().createTopicWithSchema(topicWithSchema);

        TestSubscriber subscriber = subscribers.createSubscriber();

        // and subscription with json content type
        hermes.initHelper().createSubscription(
                subscription(topic.getQualifiedName(), "subscription", subscriber.getEndpoint())
                        .withContentType(JSON)
                        .build()
        );

        // when first message is published
        hermes.api().publishAvroUntilSuccess(topic.getQualifiedName(), user.asBytes());

        // then it is consumed as json
        subscriber.waitUntilRequestReceived(request ->
                assertThatJson(request.getBodyAsString()).when(Option.IGNORING_EXTRA_FIELDS).isEqualTo(user.asJson()));
        subscriber.reset();

        //when subscription content type is changed to avro
        hermes.api().updateSubscription(topic, "subscription", patchData().set("contentType", ContentType.AVRO).build());
        long currentTime = clock.millis();
        waitUntilSubscriptionContentTypeChanged(topic, "subscription", ContentType.AVRO);
        waitUntilConsumersUpdateSubscription(currentTime, topic, "subscription");

        // and second message is published
        hermes.api().publishAvroUntilSuccess(topic.getQualifiedName(), user.asBytes());

        // then it is consumed as avro
        subscriber.waitUntilRequestReceived(request -> assertBodyDeserializesIntoUser(request.getBodyAsString(), user));
    }

    @Test
    public void shouldGetBadRequestForPublishingInvalidMessageWithSchema() {
        // given
        TopicWithSchema topicWithSchema = topicWithSchema(topicWithRandomName()
                .withContentType(AVRO)
                .build(), user.getSchemaAsString());
        Topic topic = hermes.initHelper().createTopicWithSchema(topicWithSchema);

        // when
        WebTestClient.ResponseSpec response = hermes.api().publishAvro(topic.getQualifiedName(), "invalidMessage".getBytes());

        // then
        response.expectStatus().isBadRequest();
    }

    @Test
    public void shouldIgnoreValidationDryRunSettingForAvroTopic() {
        // given
        TopicWithSchema topicWithSchema = topicWithSchema(topicWithRandomName()
                .withContentType(AVRO)
                .withJsonToAvroDryRun(true)
                .build(), user.getSchemaAsString());
        Topic topic = hermes.initHelper().createTopicWithSchema(topicWithSchema);

        // when
        WebTestClient.ResponseSpec response = hermes.api().publishAvro(topic.getQualifiedName(), "invalidMessage".getBytes());

        // then
        response.expectStatus().isBadRequest();
    }

    @Test
    public void shouldPublishJsonMessageConvertedToAvroForAvroTopics() {
        // given
        TopicWithSchema topicWithSchema = topicWithSchema(topicWithRandomName()
                .withContentType(AVRO)
                .build(), user.getSchemaAsString());
        Topic topic = hermes.initHelper().createTopicWithSchema(topicWithSchema);

        // when & then
        hermes.api().publishJSONUntilSuccess(topic.getQualifiedName(), user.asJson());
    }

    @Test
    public void shouldPublishAvroEncodedJsonMessageConvertedToAvroForAvroTopics() {
        // given
        TopicWithSchema topicWithSchema = topicWithSchema(topicWithRandomName()
                .withContentType(AVRO)
                .build(), user.getSchemaAsString());
        Topic topic = hermes.initHelper().createTopicWithSchema(topicWithSchema);

        // when & then
        hermes.api().publishUntilSuccess(topic.getQualifiedName(), user.asAvroEncodedJson(), createHeaders(singletonMap("Content-Type", AVRO_JSON)));
    }

    @Test
    public void shouldGetBadRequestForJsonNotMatchingWithAvroSchemaAndAvroContentType() {
        // given
        TopicWithSchema topicWithSchema = topicWithSchema(topicWithRandomName()
                .withContentType(AVRO)
                .build(), user.getSchemaAsString());
        Topic topic = hermes.initHelper().createTopicWithSchema(topicWithSchema);
        String message = "{\"__metadata\":null,\"name\":\"john\",\"age\":\"string instead of int\"}";

        // when / then
        waitAtMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            WebTestClient.ResponseSpec response = hermes.api().publish(topic.getQualifiedName(), message, createHeaders(Map.of("Content-Type", AVRO_JSON)));
            response.expectStatus().isBadRequest();
            response.expectBody(String.class).isEqualTo(
                    "{" + "\"message\":\"Invalid message: Failed to convert to AVRO: Expected int. Got VALUE_STRING.\","
                            + "\"code\":\"VALIDATION_ERROR\""
                            + "}"
            );
        });
    }

    @Test
    public void shouldGetBadRequestForJsonNotMachingWithAvroSchemaAndJsonContentType() {
        // given
        TopicWithSchema topicWithSchema = topicWithSchema(topicWithRandomName()
                .withContentType(AVRO)
                .build(), user.getSchemaAsString());
        Topic topic = hermes.initHelper().createTopicWithSchema(topicWithSchema);

        // when
        String message = "{\"name\":\"john\",\"age\":\"string instead of int\"}";
        WebTestClient.ResponseSpec response = hermes.api().publishJSON(topic.getQualifiedName(), message);

        // then
        response.expectStatus().isBadRequest();
        response.expectBody(String.class).isEqualTo(
                "{" + "\"message\":\"Invalid message: Failed to convert JSON to Avro: Field age is expected to be type: java.lang.Number\","
                        + "\"code\":\"VALIDATION_ERROR\""
                        + "}"
        );
    }

    @Test
    public void shouldGetBadRequestForInvalidJsonWithAvroSchema() {
        // given
        TopicWithSchema topicWithSchema = topicWithSchema(topicWithRandomName()
                .withContentType(AVRO)
                .build(), user.getSchemaAsString());
        Topic topic = hermes.initHelper().createTopicWithSchema(topicWithSchema);

        // when
        WebTestClient.ResponseSpec response = hermes.api().publishJSON(topic.getQualifiedName(), "{\"name\":\"Bob\"");

        // then
        response.expectStatus().isBadRequest();
    }

    @Test
    public void shouldReturnBadRequestOnMissingSchemaAtSpecifiedVersion() {
        TopicWithSchema topicWithSchema = topicWithSchema(topicWithRandomName()
                .withContentType(AVRO)
                .build(), user.getSchemaAsString());
        Topic topic = hermes.initHelper().createTopicWithSchema(topicWithSchema);

        // when
        HermesMessage message = hermesMessage(topic.getQualifiedName(), user.asBytes())
                .avro(2)
                .build();

        WebTestClient.ResponseSpec response = hermes.api().publishAvro(topic.getQualifiedName(), message.getBody(), createHeaders(message.getHeaders()));

        // then
        response.expectStatus().isBadRequest();
        response.expectBody(String.class)
                .isEqualTo("{\"message\":\"Given schema version '2' does not exist\",\"code\":\"SCHEMA_VERSION_DOES_NOT_EXIST\"}");
    }

    @Test
    public void shouldPublishJsonIncompatibleWithSchemaWhileJsonToAvroDryRunModeIsEnabled() {
        // given
        Topic topic = hermes.initHelper().createTopic(
                topicWithRandomName()
                        .withJsonToAvroDryRun(true)
                        .withContentType(JSON)
                        .build());
        hermes.api().ensureSchemaSaved(topic.getQualifiedName(), false, user.getSchemaAsString());

        TestSubscriber subscriber = subscribers.createSubscriber();
        hermes.initHelper().createSubscription(subscription(topic.getQualifiedName(), "subscription", subscriber.getEndpoint()).build());

        TestMessage message = TestMessage.random();

        // when
        hermes.api().publishJSONUntilSuccess(topic.getQualifiedName(), message.body());

        // then
        subscriber.waitUntilReceived(message.body());
    }

    @Test
    public void shouldPublishJsonCompatibleWithSchemaWithoutMetadataWhileJsonToAvroDryRunModeIsEnabled() {
        // given
        Topic topic = hermes.initHelper().createTopic(
                topicWithRandomName()
                        .withJsonToAvroDryRun(true)
                        .withContentType(JSON)
                        .build());

        Schema schema = AvroUserSchemaLoader.load("/schema/user_no_metadata.avsc");
        hermes.api().ensureSchemaSaved(topic.getQualifiedName(), false, schema.toString());

        TestSubscriber subscriber = subscribers.createSubscriber();
        hermes.initHelper().createSubscription(subscription(topic.getQualifiedName(), "subscription", subscriber.getEndpoint()).build());

        // when
        hermes.api().publishJSONUntilSuccess(topic.getQualifiedName(), user.asJson());

        // then
        subscriber.waitUntilReceived(user.asJson());
    }

    @Test
    public void shouldPublishAvroAndConsumeJsonMessageWithTraceId() {
        // given
        final String traceId = UUID.randomUUID().toString();

        // and
        TopicWithSchema topicWithSchema = topicWithSchema(topicWithRandomName()
                .withContentType(AVRO)
                .build(), user.getSchemaAsString());

        Topic topic = hermes.initHelper().createTopicWithSchema(topicWithSchema);

        TestSubscriber subscriber = subscribers.createSubscriber();
        hermes.initHelper().createSubscription(subscription(topic.getQualifiedName(),
                "subscription", subscriber.getEndpoint())
                .build());

        // when
        hermes.api().publishAvroUntilSuccess(topic.getQualifiedName(), user.asBytes(), createHeaders(singletonMap("Trace-Id", traceId)));

        // then
        subscriber.waitUntilRequestReceived(request -> {
            assertThat(request.getBodyAsString()).isEqualTo(user.asJson());
            assertThat(request.getHeader("Trace-Id")).isEqualTo(traceId);
        });
    }

    @Test
    public void shouldUseExplicitSchemaVersionWhenPublishingAndConsuming() {
        // given
        TopicWithSchema topicWithSchema = topicWithSchema(topicWithRandomName()
                .withContentType(AVRO)
                .withSchemaIdAwareSerialization()
                .build(), user.getSchemaAsString());

        Topic topic = hermes.initHelper().createTopicWithSchema(topicWithSchema);

        TestSubscriber subscriber = subscribers.createSubscriber();
        hermes.initHelper().createSubscription(subscription(topic.getQualifiedName(),
                "subscription", subscriber.getEndpoint())
                        .withContentType(AVRO)
                .build());

        hermes.api().ensureSchemaSaved(topic.getQualifiedName(), false, load("/schema/user_v2.avsc").toString());

        // when
        HermesMessage message = hermesMessage(topic.getQualifiedName(), user.asBytes())
                .avro(1)
                .build();

        hermes.api().publishAvroUntilSuccess(topic.getQualifiedName(), message.getBody(), createHeaders(message.getHeaders()));

        // then
        subscriber.waitUntilRequestReceived(request -> {
            assertThat(request.getHeaders().getHeader(HermesMessage.SCHEMA_VERSION_HEADER).firstValue()).isEqualTo("1");
            assertBodyDeserializesIntoUser(request.getBodyAsString(), user);
        });
    }

    @Test
    public void shouldUseExplicitSchemaVersionWhenPublishingAndConsumingWithLowercaseHeader() {
        // given
        TopicWithSchema topicWithSchema = topicWithSchema(topicWithRandomName()
                .withContentType(AVRO)
                .withSchemaIdAwareSerialization()
                .build(), user.getSchemaAsString());

        Topic topic = hermes.initHelper().createTopicWithSchema(topicWithSchema);

        TestSubscriber subscriber = subscribers.createSubscriber();
        hermes.initHelper().createSubscription(subscription(topic.getQualifiedName(),
                "subscription", subscriber.getEndpoint())
                .withContentType(AVRO)
                .build());

        hermes.api().ensureSchemaSaved(topic.getQualifiedName(), false, load("/schema/user_v2.avsc").toString());

        // when
        HermesMessage message = hermesMessage(topic.getQualifiedName(), user.asBytes())
                .withHeader("schema-version", "1")
                .build();

        hermes.api().publishAvroUntilSuccess(topic.getQualifiedName(), message.getBody(), createHeaders(message.getHeaders()));

        // then
        subscriber.waitUntilRequestReceived(request -> {
            assertThat(request.getHeaders().getHeader(HermesMessage.SCHEMA_VERSION_HEADER).firstValue()).isEqualTo("1");
            assertBodyDeserializesIntoUser(request.getBodyAsString(), user);
        });
    }

    @Test
    public void shouldUpdateSchemaAndUseItImmediately() {
        // given
        TopicWithSchema topicWithSchema = topicWithSchema(topicWithRandomName()
                .withContentType(AVRO)
                .build(), user.getSchemaAsString());

        Topic topic = hermes.initHelper().createTopicWithSchema(topicWithSchema);

        TestSubscriber subscriber = subscribers.createSubscriber();
        hermes.initHelper().createSubscription(subscription(topic.getQualifiedName(),
                "subscription", subscriber.getEndpoint())
                .withContentType(AVRO)
                .build());

        HermesMessage message = hermesMessage(topic.getQualifiedName(), user.asBytes()).build();

        // when message is published with schema version 1
        hermes.api().publishAvroUntilSuccess(topic.getQualifiedName(), message.getBody(), createHeaders(message.getHeaders()));

        // then it is consumed with schema version 1
        subscriber.waitUntilRequestReceived(request -> {
            assertThat(request.getHeaders().getHeader(HermesMessage.SCHEMA_VERSION_HEADER).firstValue()).isEqualTo("1");
            assertBodyDeserializesIntoUser(request.getBodyAsString(), user);
        });
        subscriber.reset();

        Schema schemaV2 = load("/schema/user_v2.avsc");
        AvroUser userV2 = new AvroUser(CompiledSchema.of(schemaV2, 2, 2), "Bob", 50, "blue");
        HermesMessage messageV2 = hermesMessage(topic.getQualifiedName(), userV2.asBytes()).build();

        // when schema is updated to version 2
        hermes.api().ensureSchemaSaved(topic.getQualifiedName(), false, schemaV2.toString());

        // and messages is published with schema version 2
        hermes.api().publishAvroUntilSuccess(topic.getQualifiedName(), messageV2.getBody(), createHeaders(messageV2.getHeaders()));

        // then it is consumed with schema version 2
        subscriber.waitUntilRequestReceived(request -> {
            assertThat(request.getHeaders().getHeader(HermesMessage.SCHEMA_VERSION_HEADER).firstValue()).isEqualTo("2");
            assertBodyDeserializesIntoUser(request.getBodyAsString(), userV2);
        });
    }

    @Test
    public void shouldPublishAndConsumeJsonMessageAfterMigrationFromJsonToAvro() {
        // given
        Topic topic = hermes.initHelper().createTopic(topicWithRandomName().withContentType(JSON).build());

        TestSubscriber subscriber = subscribers.createSubscriber();
        hermes.initHelper().createSubscription(subscription(topic.getQualifiedName(),
                "subscription", subscriber.getEndpoint()).build());

        final TestMessage beforeMigrationMessage = new AvroUser("Bob", 50, "blue").asTestMessage();
        final AvroUser afterMigrationMessage = new AvroUser("Barney", 35, "yellow");

        hermes.api().publishUntilSuccess(topic.getQualifiedName(), beforeMigrationMessage.body());
        subscriber.waitUntilReceived(beforeMigrationMessage.body());
        subscriber.reset();

        waitUntilConsumerCommitsOffset(topic, "subscription");

        PatchData patch = patchData()
                .set("contentType", ContentType.AVRO)
                .set("migratedFromJsonType", true)
                .set("schema", user.getSchemaAsString())
                .build();
        hermes.api().updateTopic(topic.getQualifiedName(), patch);

        // when
        hermes.api().publishJSONUntilSuccess(topic.getQualifiedName(), afterMigrationMessage.asTestMessage().withEmptyAvroMetadata().body());

        // then
        subscriber.waitUntilReceived(afterMigrationMessage.asTestMessage().body());
    }

    @Test
    public void shouldSendMessageIdHeaderToSubscriber() {
        // given
        TopicWithSchema topicWithSchema = topicWithSchema(topicWithRandomName()
                .withContentType(AVRO)
                .build(), user.getSchemaAsString());
        Topic topic = hermes.initHelper().createTopicWithSchema(topicWithSchema);
        TestSubscriber subscriber = subscribers.createSubscriber();
        hermes.initHelper().createSubscription(subscription(topic.getQualifiedName(),
                "subscription", subscriber.getEndpoint())
                .withContentType(AVRO)
                .build());
        String traceId = UUID.randomUUID().toString();

        // when
        hermes.api().publishAvroUntilSuccess(topic.getQualifiedName(), user.asBytes(), createHeaders(singletonMap("Trace-Id", traceId)));

        // then
        subscriber.waitUntilRequestReceived(request -> {
            String messageId = request.getHeader("Hermes-Message-Id");
            assertThat(messageId).isNotBlank();
            assertThat(request.getHeader("messageId")).isEqualTo(messageId);
            assertThat(request.getHeader("Trace-Id")).isEqualTo(traceId);
        });
    }

    private void assertBodyDeserializesIntoUser(String body, AvroUser user) {
        AvroUser avroUser = AvroUser.create(user.getCompiledSchema(), body.getBytes());
        assertThat(avroUser.getName()).isEqualTo(user.getName());
        assertThat(avroUser.getAge()).isEqualTo(user.getAge());
        assertThat(avroUser.getFavoriteColor()).isEqualTo(user.getFavoriteColor());
    }

    private void waitUntilSubscriptionContentTypeChanged(Topic topic, String subscription, ContentType expected) {
        waitAtMost(adjust(Duration.ofSeconds(10))).until(() -> {
            ContentType actual = hermes.api().getSubscription(topic.getQualifiedName(), subscription).getContentType();
            logger.info("Expecting {} subscription endpoint address. Actual {}", expected, actual);
            return expected.equals(actual);
        });
    }

    private void waitUntilConsumerCommitsOffset(Topic topic, String subscription) {
        long currentTime = clock.millis();
        waitAtMost(adjust(Duration.ofMinutes(1))).until(() ->
        hermes.api().getRunningSubscriptionsStatus().stream()
                .filter(sub -> sub.getQualifiedName().equals(topic.getQualifiedName() + "$" + subscription))
                .anyMatch(sub -> sub.getSignalTimesheet().getOrDefault(COMMIT, 0L) > currentTime));

    }

    private void waitUntilConsumersUpdateSubscription(final long currentTime, Topic topic, String subscription) {
        waitAtMost(adjust(Duration.ofSeconds(10))).until(() ->
                hermes.api().getRunningSubscriptionsStatus().stream()
                        .filter(sub -> sub.getQualifiedName().equals(topic.getQualifiedName() + "$" + subscription))
                        .anyMatch(sub -> sub.getSignalTimesheet().getOrDefault(UPDATE_SUBSCRIPTION, 0L) > currentTime));
    }
}
