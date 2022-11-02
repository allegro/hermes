package pl.allegro.tech.hermes.integration;

import net.javacrumbs.jsonunit.core.Option;
import org.apache.avro.Schema;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import pl.allegro.tech.hermes.api.ContentType;
import pl.allegro.tech.hermes.api.PatchData;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.client.HermesMessage;
import pl.allegro.tech.hermes.integration.env.SharedServices;
import pl.allegro.tech.hermes.integration.shame.Unreliable;
import pl.allegro.tech.hermes.schema.CompiledSchema;
import pl.allegro.tech.hermes.test.helper.avro.AvroUser;
import pl.allegro.tech.hermes.test.helper.avro.AvroUserSchemaLoader;
import pl.allegro.tech.hermes.test.helper.endpoint.RemoteServiceEndpoint;
import pl.allegro.tech.hermes.test.helper.message.TestMessage;

import java.time.Clock;
import java.util.UUID;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static java.util.Collections.singletonMap;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.OK;
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson;
import static pl.allegro.tech.hermes.api.AvroMediaType.AVRO_BINARY;
import static pl.allegro.tech.hermes.api.AvroMediaType.AVRO_JSON;
import static pl.allegro.tech.hermes.api.ContentType.AVRO;
import static pl.allegro.tech.hermes.api.ContentType.JSON;
import static pl.allegro.tech.hermes.api.PatchData.patchData;
import static pl.allegro.tech.hermes.api.TopicWithSchema.topicWithSchema;
import static pl.allegro.tech.hermes.client.HermesMessage.hermesMessage;
import static pl.allegro.tech.hermes.integration.test.HermesAssertions.assertThat;
import static pl.allegro.tech.hermes.test.helper.avro.AvroUserSchemaLoader.load;
import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.randomTopic;

public class PublishingAvroTest extends IntegrationTest {

    private RemoteServiceEndpoint remoteService;
    private AvroUser user;
    private final Clock clock = Clock.systemDefaultZone();

    @BeforeClass
    public void initialize() {
        user = new AvroUser("Bob", 50, "blue");
    }

    @BeforeMethod
    public void initializeAlways() {
        this.remoteService = new RemoteServiceEndpoint(SharedServices.services().serviceMock());
    }

    @Test
    public void shouldPublishAvroAndConsumeJsonMessage() {
        // given
        Topic topic = randomTopic("publishAvroConsumeJson", "topic")
                .withContentType(AVRO)
                .build();
        operations.buildTopicWithSchema(topicWithSchema(topic, user.getSchemaAsString()));
        operations.createSubscription(topic, "subscription", remoteService.getUrl());

        // when
        Response response = publisher.publish(topic.getQualifiedName(), user.asBytes());
        assertThat(response).hasStatus(CREATED);

        // then
        remoteService.waitUntilReceived(json -> assertThatJson(json).isEqualTo(user.asJson()));
    }

    @Test
    public void shouldNotPublishAvroWhenMessageIsNotJsonOrAvro() {
        // given
        Topic topic = randomTopic("notPublishAvroConsumeAvro", "topic")
                .withContentType(AVRO)
                .build();
        operations.buildTopicWithSchema(topicWithSchema(topic, user.getSchemaAsString()));
        operations.createSubscription(topic, "subscription", remoteService.getUrl(), ContentType.AVRO);

        // when
        Response response = publisher.publish(topic.getQualifiedName(), user.asJson(), singletonMap("Content-Type", TEXT_PLAIN));

        // then
        assertThat(response).hasStatus(BAD_REQUEST);
    }

    @Test
    public void shouldPublishAvroAndConsumeAvroMessage() {
        // given
        Topic topic = randomTopic("publishAvroConsumeAvro", "topic")
                .withContentType(AVRO)
                .build();
        operations.buildTopicWithSchema(topicWithSchema(topic, user.getSchemaAsString()));
        operations.createSubscription(topic, "subscription", remoteService.getUrl(), ContentType.AVRO);

        // when
        Response response = publisher.publish(topic.getQualifiedName(), user.asBytes());
        assertThat(response).hasStatus(CREATED);

        // then
        remoteService.waitUntilReceived(body -> assertBodyDeserializesIntoUser(body, user));
    }

    @Test
    public void shouldSendAvroAfterSubscriptionContentTypeChanged() {
        // given
        Topic topic = randomTopic("publishAvroAfterTopicEditing", "topic").withContentType(AVRO).build();
        operations.buildTopicWithSchema(topicWithSchema(topic, user.getSchemaAsString()));
        operations.createSubscription(topic, "subscription", remoteService.getUrl(), JSON);

        Response response = publisher.publish(topic.getQualifiedName(), user.asBytes());
        assertThat(response).hasStatus(CREATED);

        remoteService.waitUntilReceived(json -> assertThatJson(json).when(Option.IGNORING_EXTRA_FIELDS).isEqualTo(user.asJson()));
        remoteService.reset();

        //when
        assertThat(management.subscription()
                .update(topic.getQualifiedName(), "subscription", patchData().set("contentType", ContentType.AVRO).build()))
                .hasStatus(OK);

        long currentTime = clock.millis();
        wait.untilSubscriptionContentTypeChanged(topic, "subscription", ContentType.AVRO);
        wait.untilConsumersUpdateSubscription(currentTime, topic, "subscription");

        publisher.publish(topic.getQualifiedName(), user.asBytes());

        //then
        remoteService.waitUntilReceived(body -> assertBodyDeserializesIntoUser(body, user));
    }

    @Test
    public void shouldGetBadRequestForPublishingInvalidMessageWithSchema() {
        // given
        Topic topic = randomTopic("invalidAvro", "topic").withContentType(AVRO).build();
        operations.buildTopicWithSchema(topicWithSchema(topic, user.getSchemaAsString()));

        // when
        Response response = publisher.publish(topic.getQualifiedName(), "invalidMessage".getBytes());

        // then
        assertThat(response.getStatus()).isEqualTo(BAD_REQUEST.getStatusCode());
    }

    @Test
    public void shouldIgnoreValidationDryRunSettingForAvroTopic() {
        // given
        Topic topic = randomTopic("invalidAvro", "topicWithValidationDryRun")
                .withContentType(AVRO).build();
        operations.buildTopicWithSchema(topicWithSchema(topic, user.getSchemaAsString()));

        // when
        Response response = publisher.publish(topic.getQualifiedName(), "invalidMessage".getBytes());

        // then
        assertThat(response.getStatus()).isEqualTo(BAD_REQUEST.getStatusCode());
    }

    @Test
    public void shouldPublishJsonMessageConvertedToAvroForAvroTopics() {
        // given
        Topic topic = randomTopic("avro", "topic2").withContentType(AVRO).build();
        operations.buildTopicWithSchema(topicWithSchema(topic, user.getSchemaAsString()));

        // when
        Response response = publisher.publish(topic.getQualifiedName(), user.asJson());

        // then
        assertThat(response.getStatus()).isEqualTo(CREATED.getStatusCode());
    }

    @Test
    public void shouldPublishAvroEncodedJsonMessageConvertedToAvroForAvroTopics() {
        // given
        Topic topic = randomTopic("avro", "topic2").withContentType(AVRO).build();
        operations.buildTopicWithSchema(topicWithSchema(topic, user.getSchemaAsString()));

        // when
        Response response = publisher.publish(topic.getQualifiedName(), user.asAvroEncodedJson(), singletonMap("Content-Type", AVRO_JSON));

        // then
        assertThat(response.getStatus()).isEqualTo(CREATED.getStatusCode());
    }

    @Test
    public void shouldGetBadRequestForJsonNotMachingWithAvroSchemaAndAvroContentType() {
        // given
        Topic topic = randomTopic("pl.allegro", "User").withContentType(AVRO).build();
        Schema schema = AvroUserSchemaLoader.load("/schema/user.avsc");
        operations.buildTopicWithSchema(topicWithSchema(topic, schema.toString()));

        // when
        String message = "{\"__metadata\":null,\"name\":\"john\",\"age\":\"string instead of int\"}";
        Response response = publisher.publish(topic.getQualifiedName(), message, singletonMap("Content-Type", AVRO_JSON));

        // then
        assertThat(response.getStatus()).isEqualTo(BAD_REQUEST.getStatusCode());
        assertThat(response.readEntity(String.class)).isEqualTo(
                "{" + "\"message\":\"Invalid message: Failed to convert to AVRO: Expected int. Got VALUE_STRING.\","
                        + "\"code\":\"VALIDATION_ERROR\""
                        + "}"
        );
    }

    @Test
    public void shouldGetBadRequestForJsonNotMachingWithAvroSchemaAndJsonContentType() {
        // given
        Topic topic = randomTopic("pl.allegro", "User").withContentType(AVRO).build();
        Schema schema = AvroUserSchemaLoader.load("/schema/user.avsc");
        operations.buildTopicWithSchema(topicWithSchema(topic, schema.toString()));

        // when
        String message = "{\"name\":\"john\",\"age\":\"string instead of int\"}";
        Response response = publisher.publish(topic.getQualifiedName(), message, singletonMap("Content-Type", "application/json"));

        // then
        assertThat(response.getStatus()).isEqualTo(BAD_REQUEST.getStatusCode());
        assertThat(response.readEntity(String.class)).isEqualTo(
                "{" + "\"message\":\"Invalid message: Failed to convert JSON to Avro: Field age is expected to be type: java.lang.Number\","
                        + "\"code\":\"VALIDATION_ERROR\""
                        + "}"
        );
    }

    @Test
    public void shouldGetBadRequestForInvalidJsonWithAvroSchema() {
        Topic topic = randomTopic("avro", "invalidJson").withContentType(AVRO).build();
        operations.buildTopicWithSchema(topicWithSchema(topic, user.getSchemaAsString()));

        // when
        Response response = publisher.publish(topic.getQualifiedName(), "{\"name\":\"Bob\"");

        // then
        assertThat(response.getStatus()).isEqualTo(BAD_REQUEST.getStatusCode());
    }

    @Test
    public void shouldReturnBadRequestOnMissingSchemaAtSpecifiedVersion() {
        Topic topic = randomTopic("avro", "topicWithoutSchemaAtSpecifiedVersion").withContentType(AVRO).build();
        operations.buildTopicWithSchema(topicWithSchema(topic, user.getSchemaAsString()));

        // when
        HermesMessage message = hermesMessage(topic.getQualifiedName(), user.asBytes())
                .avro(2)
                .build();

        Response response = publisher.publishAvro(topic.getQualifiedName(), message.getBody(), message.getHeaders());

        // then
        assertThat(response.getStatus()).isEqualTo(BAD_REQUEST.getStatusCode());
        assertThat(response.readEntity(String.class))
                .isEqualTo("{\"message\":\"Given schema version '2' does not exist\",\"code\":\"SCHEMA_VERSION_DOES_NOT_EXIST\"}");
    }

    @Test
    public void shouldPublishJsonIncompatibleWithSchemaWhileJsonToAvroDryRunModeIsEnabled() {
        // given
        Topic topic = randomTopic("jsonToAvroDryRun", "topic")
                .withJsonToAvroDryRun(true)
                .withContentType(JSON)
                .build();
        operations.buildTopic(topic);
        operations.saveSchema(topic, user.getSchemaAsString());

        operations.createSubscription(topic, "subscription", remoteService.getUrl());
        TestMessage message = TestMessage.random();
        remoteService.expectMessages(message);

        // when
        Response response = publisher.publish(topic.getQualifiedName(), message.body());
        assertThat(response).hasStatus(CREATED);

        // then
        remoteService.waitUntilReceived();
    }

    @Test
    public void shouldPublishJsonCompatibleWithSchemaWithoutMetadataWhileJsonToAvroDryRunModeIsEnabled() {
        // given
        Schema schema = AvroUserSchemaLoader.load("/schema/user_no_metadata.avsc");
        Topic topic = randomTopic("jsonToAvroDryRun", "topic2")
                .withJsonToAvroDryRun(true)
                .withContentType(JSON)
                .build();
        operations.buildTopic(topic);
        operations.saveSchema(topic, schema.toString());

        operations.createSubscription(topic, "subscription", remoteService.getUrl());
        remoteService.expectMessages(user.asJson());

        // when
        Response response = publisher.publish(topic.getQualifiedName(), user.asJson());
        assertThat(response).hasStatus(CREATED);

        // then
        remoteService.waitUntilReceived();
    }

    @Test
    @Unreliable
    public void shouldPublishAndConsumeJsonMessageAfterMigrationFromJsonToAvro() {
        // given
        Topic topic = operations.buildTopic(randomTopic("migrated", "topic")
                .withContentType(JSON)
                .build()
        );
        operations.createSubscription(topic, "subscription", remoteService.getUrl());

        TestMessage beforeMigrationMessage = new AvroUser("Bob", 50, "blue").asTestMessage();
        TestMessage afterMigrationMessage = new AvroUser("Barney", 35, "yellow").asTestMessage();

        remoteService.expectMessages(beforeMigrationMessage, afterMigrationMessage);

        publisher.publish(topic.getQualifiedName(), beforeMigrationMessage.body());
        wait.untilConsumerCommitsOffset(topic, "subscription");

        PatchData patch = patchData()
                .set("contentType", ContentType.AVRO)
                .set("migratedFromJsonType", true)
                .set("schema", user.getSchemaAsString())
                .build();
        operations.updateTopic(topic.getName(), patch);

        // when
        Response response = publisher.publish(topic.getQualifiedName(), afterMigrationMessage.withEmptyAvroMetadata().body());
        assertThat(response).hasStatus(CREATED);

        // then
        remoteService.waitUntilReceived();
    }

    @Test
    public void shouldPublishAvroAndConsumeJsonMessageWithTraceId() {

        // given
        final String traceId = UUID.randomUUID().toString();

        // and
        Topic topic = randomTopic("publishAvroConsumeJsonWithTraceId", "topic").withContentType(AVRO).build();
        operations.buildTopicWithSchema(topicWithSchema(topic, user.getSchemaAsString()));
        operations.createSubscription(topic, "subscription", remoteService.getUrl());

        remoteService.expectMessages(user.asJson());
        WebTarget client = ClientBuilder.newClient().target(FRONTEND_URL).path("topics").path(topic.getQualifiedName());

        // when
        Response response = client
                .request()
                .header("Trace-Id", traceId)
                .post(Entity.entity(user.asBytes(), MediaType.valueOf("avro/binary")));

        // then
        assertThat(response).hasStatus(Response.Status.CREATED);
        assertThat(remoteService.waitAndGetLastRequest()).hasHeaderValue("Trace-Id", traceId);
    }

    @Test
    public void shouldUseExplicitSchemaVersionWhenPublishingAndConsuming() {
        // given
        Topic topic = randomTopic("explicitSchemaVersion", "topic")
                .withContentType(AVRO)
                .withSchemaIdAwareSerialization()
                .build();
        operations.buildTopicWithSchema(topicWithSchema(topic, load("/schema/user.avsc").toString()));
        operations.createSubscription(topic, "subscription", remoteService.getUrl(), ContentType.AVRO);

        operations.saveSchema(topic, load("/schema/user_v2.avsc").toString());

        // when
        HermesMessage message = hermesMessage(topic.getQualifiedName(), user.asBytes())
                .avro(1)
                .build();

        assertThat(publisher.publishAvro(topic.getQualifiedName(), message.getBody(), message.getHeaders())).hasStatus(CREATED);

        // then
        remoteService.waitUntilRequestReceived(request -> {
            assertThat(request.getHeaders().getHeader(HermesMessage.SCHEMA_VERSION_HEADER).firstValue()).isEqualTo("1");
            assertBodyDeserializesIntoUser(request.getBodyAsString(), user);
        });
    }

    @Test
    public void shouldUseExplicitSchemaVersionWhenPublishingAndConsumingWithLowercaseHeader() {
        // given
        Topic topic = randomTopic("explicitSchemaVersion", "topic")
                .withContentType(AVRO)
                .withSchemaIdAwareSerialization()
                .build();
        operations.buildTopicWithSchema(topicWithSchema(topic, load("/schema/user.avsc").toString()));
        operations.createSubscription(topic, "subscription", remoteService.getUrl(), ContentType.AVRO);

        operations.saveSchema(topic, load("/schema/user_v2.avsc").toString());

        // when
        HermesMessage message = hermesMessage(topic.getQualifiedName(), user.asBytes())
                .withContentType(AVRO_BINARY)
                .withHeader("schema-version", "1")
                .build();

        assertThat(publisher.publishAvro(topic.getQualifiedName(), message.getBody(), message.getHeaders())).hasStatus(CREATED);

        // then
        remoteService.waitUntilRequestReceived(request -> assertBodyDeserializesIntoUser(request.getBodyAsString(), user));
    }

    @Test
    public void shouldUpdateSchemaAndUseItImmediately() {
        // given
        Topic topic = randomTopic("latestSchemaVersionUpdate", "topic").withContentType(AVRO).build();

        operations.buildTopicWithSchema(topicWithSchema(topic, load("/schema/user.avsc").toString()));
        operations.createSubscription(topic, "subscription", remoteService.getUrl(), ContentType.AVRO);

        HermesMessage message = hermesMessage(topic.getQualifiedName(), user.asBytes()).withContentType(AVRO_BINARY).build();

        assertThat(publisher.publishAvro(topic.getQualifiedName(), message.getBody(), message.getHeaders())).hasStatus(CREATED);

        remoteService.waitUntilRequestReceived(request -> {
            assertThat(request.getHeaders().getHeader(HermesMessage.SCHEMA_VERSION_HEADER).firstValue()).isEqualTo("1");
            assertBodyDeserializesIntoUser(request.getBodyAsString(), user);
        });
        remoteService.reset();

        Schema schemaV2 = load("/schema/user_v2.avsc");
        AvroUser userV2 = new AvroUser(CompiledSchema.of(schemaV2, 2, 2), "Bob", 50, "blue");
        HermesMessage messageV2 = hermesMessage(topic.getQualifiedName(), userV2.asBytes()).withContentType(AVRO_BINARY).build();

        // when
        operations.saveSchema(topic, schemaV2.toString());

        // and
        assertThat(publisher.publishAvro(topic.getQualifiedName(), messageV2.getBody(), messageV2.getHeaders())).hasStatus(CREATED);

        // then
        remoteService.waitUntilRequestReceived(request -> {
            assertThat(request.getHeaders().getHeader(HermesMessage.SCHEMA_VERSION_HEADER).firstValue()).isEqualTo("2");
            assertBodyDeserializesIntoUser(request.getBodyAsString(), userV2);
        });
    }

    private void assertBodyDeserializesIntoUser(String body, AvroUser user) {
        AvroUser avroUser = AvroUser.create(user.getCompiledSchema(), body.getBytes());
        assertThat(avroUser.getName()).isEqualTo(user.getName());
        assertThat(avroUser.getAge()).isEqualTo(user.getAge());
        assertThat(avroUser.getFavoriteColor()).isEqualTo(user.getFavoriteColor());
    }

}
