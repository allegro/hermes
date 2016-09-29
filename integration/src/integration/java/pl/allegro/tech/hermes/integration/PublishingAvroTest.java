package pl.allegro.tech.hermes.integration;

import net.javacrumbs.jsonunit.core.Option;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import pl.allegro.tech.hermes.api.ContentType;
import pl.allegro.tech.hermes.api.PatchData;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.integration.env.SharedServices;
import pl.allegro.tech.hermes.test.helper.avro.AvroUser;
import pl.allegro.tech.hermes.test.helper.endpoint.RemoteServiceEndpoint;
import pl.allegro.tech.hermes.test.helper.message.TestMessage;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static com.google.common.collect.ImmutableMap.of;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.OK;
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson;
import static pl.allegro.tech.hermes.api.ContentType.AVRO;
import static pl.allegro.tech.hermes.api.ContentType.JSON;
import static pl.allegro.tech.hermes.api.PatchData.patchData;
import static pl.allegro.tech.hermes.common.http.MessageMetadataHeaders.SCHEMA_VERSION;
import static pl.allegro.tech.hermes.integration.test.HermesAssertions.assertThat;
import static pl.allegro.tech.hermes.test.helper.avro.AvroUserSchemaLoader.load;
import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.topic;

public class PublishingAvroTest extends IntegrationTest {

    private RemoteServiceEndpoint remoteService;

    private AvroUser user;

    @BeforeClass
    public void initialize() throws Exception {
        user = new AvroUser("Bob", 50, "blue");
    }

    @BeforeMethod
    public void initializeAlways() {
        this.remoteService = new RemoteServiceEndpoint(SharedServices.services().serviceMock());
    }

    @Test
    public void shouldPublishAvroAndConsumeJsonMessage() throws InterruptedException, ExecutionException, TimeoutException, IOException {
        // given
        Topic topic = operations.buildTopic(topic("publishAvroConsumeJson.topic")
                .withContentType(AVRO)
                .build()
        );
        operations.saveSchema(topic, user.getSchemaAsString());
        operations.createSubscription(topic, "subscription", HTTP_ENDPOINT_URL);

        // when
        Response response = publisher.publish("publishAvroConsumeJson.topic", user.asBytes());
        assertThat(response).hasStatus(CREATED);

        // then
        remoteService.waitUntilReceived(json -> assertThatJson(json).isEqualTo(user.asJson()));
    }

    @Test
    public void shouldPublishAvroAndConsumeAvroMessage() throws InterruptedException, ExecutionException, TimeoutException, IOException {
        // given
        Topic topic = operations.buildTopic(topic("publishAvroConsumeAvro.topic")
                .withContentType(AVRO)
                .build()
        );
        operations.saveSchema(topic, user.getSchemaAsString());
        operations.createSubscription(topic, "subscription", HTTP_ENDPOINT_URL, ContentType.AVRO);

        // when
        Response response = publisher.publish("publishAvroConsumeAvro.topic", user.asBytes());
        assertThat(response).hasStatus(CREATED);

        // then
        remoteService.waitUntilReceived(body -> assertBodyDeserializesIntoUser(body, user));
    }

    @Test
    public void shouldSendAvroAfterSubscriptionContentTypeChanged() throws Exception {
        // given
        Topic topic = operations.buildTopic(topic("publishAvroAfterTopicEditing.topic")
                .withContentType(AVRO)
                .build()
        );
        operations.saveSchema(topic, user.getSchemaAsString());
        operations.createSubscription(topic, "subscription", HTTP_ENDPOINT_URL, JSON);

        Response response = publisher.publish("publishAvroAfterTopicEditing.topic", user.asBytes());
        assertThat(response).hasStatus(CREATED);

        remoteService.waitUntilReceived(json -> assertThatJson(json).when(Option.IGNORING_EXTRA_FIELDS).isEqualTo(user.asJson()));
        remoteService.reset();

        //when
        assertThat(management.subscription()
                .update(topic.getQualifiedName(), "subscription", patchData().set("contentType", ContentType.AVRO).build()))
                .hasStatus(OK);

        wait.untilSubscriptionContentTypeChanged(topic, "subscription", ContentType.AVRO);
        wait.untilSubscriptionIsActivated(topic, "subscription");
        wait.untilConsumersUpdateSubscription();

        publisher.publish("publishAvroAfterTopicEditing.topic", user.asBytes());

        //then
        remoteService.waitUntilReceived(body -> assertBodyDeserializesIntoUser(body, user));
    }

    @Test
    public void shouldGetBadRequestForPublishingInvalidMessageWithSchema() throws InterruptedException, ExecutionException, TimeoutException {
        // given
        Topic topic = operations.buildTopic(topic("invalidAvro.topic")
                .withContentType(AVRO).build());
        operations.saveSchema(topic, user.getSchemaAsString());

        // when
        Response response = publisher.publish("invalidAvro.topic", "invalidMessage".getBytes());

        // then
        assertThat(response.getStatus()).isEqualTo(BAD_REQUEST.getStatusCode());
    }

    @Test
    public void shouldIgnoreValidationDryRunSettingForAvroTopic() {
        // given
        Topic topic = operations.buildTopic(topic("invalidAvro.topicWithValidationDryRun")
                .withContentType(AVRO).build());
        operations.saveSchema(topic, user.getSchemaAsString());

        // when
        Response response = publisher.publish("invalidAvro.topicWithValidationDryRun", "invalidMessage".getBytes());

        // then
        assertThat(response.getStatus()).isEqualTo(BAD_REQUEST.getStatusCode());
    }

    @Test
    public void shouldPublishJsonMessageConvertedToAvroForAvroTopics() {
        // given
        Topic topic = operations.buildTopic(topic("avro.topic2")
                .withContentType(AVRO)
                .build()
        );
        operations.saveSchema(topic, user.getSchemaAsString());

        // when
        Response response = publisher.publish("avro.topic2", user.asJson());

        // then
        assertThat(response.getStatus()).isEqualTo(CREATED.getStatusCode());
    }

    @Test
    public void shouldGetBadRequestForJsonInvalidWithAvroSchema() {
        Topic topic = topic("avro.invalidJson")
                .withContentType(AVRO).build();
        operations.buildTopic(topic);
        operations.saveSchema(topic, user.getSchemaAsString());

        // when
        Response response = publisher.publish("avro.invalidJson", "{\"name\":\"Bob\"");

        // then
        assertThat(response.getStatus()).isEqualTo(BAD_REQUEST.getStatusCode());
    }

    @Test
    public void shouldPublishJsonIncompatibleWithSchemaWhileJsonToAvroDryRunModeIsEnabled() {
        // given
        Topic topic = topic("jsonToAvroDryRun.topic")
                .withJsonToAvroDryRun(true)
                .withContentType(JSON)
                .build();
        operations.buildTopic(topic);
        operations.saveSchema(topic, user.getSchemaAsString());

        operations.createSubscription(topic, "subscription", HTTP_ENDPOINT_URL);
        TestMessage message = TestMessage.random();
        remoteService.expectMessages(message);

        // when
        Response response = publisher.publish(topic.getQualifiedName(), message.body());
        assertThat(response).hasStatus(CREATED);

        // then
        remoteService.waitUntilReceived();
    }

    @Test
    public void shouldPublishAndConsumeJsonMessageAfterMigrationFromJsonToAvro() throws Exception {
        // given
        Topic topic = operations.buildTopic(topic("migrated", "topic")
                .withContentType(JSON)
                .build()
        );
        operations.createSubscription(topic, "subscription", HTTP_ENDPOINT_URL);

        TestMessage beforeMigrationMessage = new AvroUser("Bob", 50, "blue").asTestMessage();
        TestMessage afterMigrationMessage = new AvroUser("Barney", 35, "yellow").asTestMessage();

        remoteService.expectMessages(beforeMigrationMessage, afterMigrationMessage);

        publisher.publish(topic.getQualifiedName(), beforeMigrationMessage.body());
        wait.untilConsumerCommitsOffset();

        PatchData patch = patchData()
                .set("contentType", ContentType.AVRO)
                .set("migratedFromJsonType", true)
                .build();
        operations.saveSchema(topic, user.getSchemaAsString());
        operations.updateTopic("migrated", "topic", patch);

        // when
        Response response = publisher.publish(topic.getQualifiedName(), afterMigrationMessage.withEmptyAvroMetadata().body());
        assertThat(response).hasStatus(CREATED);

        // then
        remoteService.waitUntilReceived();
    }

    @Test
    public void shouldPublishAvroAndConsumeJsonMessageWithTraceId() throws IOException {

        // given
        String traceId = UUID.randomUUID().toString();

        // and
        Topic topic = operations.buildTopic(topic("publishAvroConsumeJsonWithTraceId.topic")
                .withContentType(AVRO).build()
        );
        operations.saveSchema(topic, user.getSchemaAsString());
        operations.createSubscription(topic, "subscription", HTTP_ENDPOINT_URL);

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
    public void shouldUseExplicitSchemaVersionWhenPublishingAndConsuming() throws IOException {
        // given
        Topic topic = operations.buildTopic(topic("explicitSchemaVersion.topic")
                .withContentType(AVRO)
                .withSchemaVersionAwareSerialization()
                .build()
        );
        operations.createSubscription(topic, "subscription", HTTP_ENDPOINT_URL, ContentType.AVRO);

        operations.saveSchema(topic, load("/schema/user.avsc").toString());
        operations.saveSchema(topic, load("/schema/user_v2.avsc").toString());

        // when
        assertThat(publisher.publishAvro(topic.getQualifiedName(), user.asBytes(), of(SCHEMA_VERSION.getName(), "1")))
                .hasStatus(CREATED);

        // then
        remoteService.waitUntilRequestReceived(request -> {
            assertThat(request.getHeaders().getHeader(SCHEMA_VERSION.getName()).firstValue()).isEqualTo("1");
            assertBodyDeserializesIntoUser(request.getBodyAsString(), user);
        });
    }

    private void assertBodyDeserializesIntoUser(String body, AvroUser user) {
        AvroUser avroUser = AvroUser.create(user.getCompiledSchema(), body.getBytes());
        assertThat(avroUser.getName()).isEqualTo(user.getName());
        assertThat(avroUser.getAge()).isEqualTo(user.getAge());
        assertThat(avroUser.getFavoriteColor()).isEqualTo(user.getFavoriteColor());
    }

}
