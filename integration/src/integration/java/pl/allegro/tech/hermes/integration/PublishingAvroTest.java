package pl.allegro.tech.hermes.integration;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import pl.allegro.tech.hermes.api.ContentType;
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

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.OK;
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson;
import static pl.allegro.tech.hermes.api.ContentType.AVRO;
import static pl.allegro.tech.hermes.api.Subscription.Builder.subscription;
import static pl.allegro.tech.hermes.api.Topic.Builder.topic;
import static pl.allegro.tech.hermes.api.ContentType.JSON;
import static pl.allegro.tech.hermes.integration.test.HermesAssertions.assertThat;

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
        Topic topic = operations.buildTopic(topic()
                .withName("publishAvroConsumeJson.topic")
                .withValidation(true)
                .withMessageSchema(user.getSchemaAsString())
                .withContentType(AVRO).build());
        operations.createSubscription(topic, "subscription", HTTP_ENDPOINT_URL);

        // when
        Response response = publisher.publish("publishAvroConsumeJson.topic", user.asBytes());

        // then
        assertThat(response.getStatus()).isEqualTo(CREATED.getStatusCode());
        remoteService.waitUntilReceived(json -> assertThatJson(json).isEqualTo("{\"name\":\"Bob\",\"age\":50,\"favoriteColor\":\"blue\"}"));
    }

    @Test
    public void shouldPublishAvroAndConsumeAvroMessage() throws InterruptedException, ExecutionException, TimeoutException, IOException {
        // given
        Topic topic = operations.buildTopic(topic()
                .withName("publishAvroConsumeAvro.topic")
                .withValidation(true)
                .withMessageSchema(user.getSchemaAsString())
                .withContentType(AVRO).build());
        operations.createSubscription(topic, "subscription", HTTP_ENDPOINT_URL, ContentType.AVRO);

        // when
        Response response = publisher.publish("publishAvroConsumeAvro.topic", user.asBytes());

        // then
        assertThat(response.getStatus()).isEqualTo(CREATED.getStatusCode());
        remoteService.waitUntilReceived(body -> {
            AvroUser avroUser = AvroUser.create(user.getSchema(), body.getBytes());
            assertThat(avroUser.getName()).isEqualTo("Bob");
            assertThat(avroUser.getAge()).isEqualTo(50);
            assertThat(avroUser.getFavoriteColor()).isEqualTo("blue");
        });
    }

    @Test
    public void shouldConsumeAvroAfterSubscriptionContentTypeChanged() throws InterruptedException, ExecutionException, TimeoutException, IOException {
        // given
        Topic topic = operations.buildTopic(topic()
                .withName("publishAvroAfterTopicEditing.topic")
                .withValidation(true)
                .withMessageSchema(user.getSchemaAsString())
                .withContentType(AVRO).build());

        operations.createSubscription(topic, "subscription", HTTP_ENDPOINT_URL);

        // when
        assertThat(publisher.publish("publishAvroAfterTopicEditing.topic", user.asBytes())).hasStatus(CREATED);

        // then
        remoteService.waitUntilReceived(json -> assertThatJson(json).isEqualTo("{\"name\":\"Bob\",\"age\":50,\"favoriteColor\":\"blue\"}"));
        remoteService.reset();

        //when
        assertThat(management.subscription()
                .update(topic.getQualifiedName(), "subscription", subscription().withContentType(ContentType.AVRO).build()))
                .hasStatus(OK);

        wait.untilSubscriptionContentTypeChanged(topic, "subscription", ContentType.AVRO);
        wait.untilSubscriptionIsActivated(topic, "subscription");

        assertThat(publisher.publish("publishAvroAfterTopicEditing.topic", user.asBytes())).hasStatus(CREATED);

        //then
        remoteService.waitUntilReceived(body -> {
            System.out.println(body);
            AvroUser avroUser = AvroUser.create(user.getSchema(), body.getBytes());
            assertThat(avroUser.getName()).isEqualTo("Bob");
            assertThat(avroUser.getAge()).isEqualTo(50);
            assertThat(avroUser.getFavoriteColor()).isEqualTo("blue");
        });
    }

    @Test
    public void shouldGetBadRequestForPublishingInvalidMessageWithSchema() throws InterruptedException, ExecutionException, TimeoutException {
        // given
        operations.buildTopic(topic()
                .withName("invalidAvro.topic")
                .withValidation(true)
                .withMessageSchema(user.getSchemaAsString())
                .withContentType(AVRO).build());

        // when
        Response response = publisher.publish("invalidAvro.topic", "invalidMessage".getBytes());

        // then
        assertThat(response.getStatus()).isEqualTo(BAD_REQUEST.getStatusCode());
    }

    @Test
    public void shouldIgnoreValidationDryRunSettingForAvroTopic() {
        // given
        operations.buildTopic(topic()
                .withName("invalidAvro.topicWithValidationDryRun")
                .withValidation(true)
                .withValidationDryRun(true)
                .withMessageSchema(user.getSchemaAsString())
                .withContentType(AVRO).build());

        // when
        Response response = publisher.publish("invalidAvro.topicWithValidationDryRun", "invalidMessage".getBytes());

        // then
        assertThat(response.getStatus()).isEqualTo(BAD_REQUEST.getStatusCode());
    }

    @Test
    public void shouldPublishJsonMessageConvertedToAvroForAvroTopics() {
        // given
        Topic topic = topic()
                .withName("avro.topic2")
                .withValidation(true)
                .withMessageSchema(user.getSchemaAsString())
                .withContentType(AVRO).build();
        operations.buildTopic(topic);

        // when
        Response response = publisher.publish("avro.topic2", "{\"name\":\"Bob\",\"age\":50,\"favoriteColor\":\"blue\"}");

        // then
        assertThat(response.getStatus()).isEqualTo(CREATED.getStatusCode());
    }

    @Test
    public void shouldGetBadRequestForJsonInvalidWithAvroSchema() {
        Topic topic = topic()
                .withName("avro.invalidJson")
                .withValidation(true)
                .withMessageSchema(user.getSchemaAsString())
                .withContentType(AVRO).build();
        operations.buildTopic(topic);

        // when
        Response response = publisher.publish("avro.invalidJson", "{\"name\":\"Bob\"");

        // then
        assertThat(response.getStatus()).isEqualTo(BAD_REQUEST.getStatusCode());
    }

    @Test
    public void shouldPublishJsonIncompatibleWithSchemaWhileJsonToAvroDryRunModeIsEnabled() {
        // given
        Topic topic = topic()
                .withName("jsonToAvroDryRun.topic")
                .withJsonToAvroDryRun(true)
                .withMessageSchema(user.getSchemaAsString())
                .withContentType(JSON)
                .build();
        operations.buildTopic(topic);

        operations.createSubscription(topic, "subscription", HTTP_ENDPOINT_URL);
        TestMessage message = TestMessage.random();
        remoteService.expectMessages(message);

        // when
        Response response = publisher.publish(topic.getQualifiedName(), message.body());

        // then
        assertThat(response).hasStatus(CREATED);
        remoteService.waitUntilReceived();
    }

    @Test
    public void shouldPublishAndConsumeJsonMessageAfterMigrationFromJsonToAvro() throws Exception {
        // given
        Topic topic = operations.buildTopic(topic().withName("migrated", "topic").applyDefaults().withContentType(JSON).build());
        operations.createSubscription(topic, "subscription", HTTP_ENDPOINT_URL);

        TestMessage beforeMigrationMessage = new AvroUser("Bob", 50, "blue").asTestMessage();
        TestMessage afterMigrationMessage =  new AvroUser("Barney", 35, "yellow").asTestMessage();

        remoteService.expectMessages(beforeMigrationMessage, afterMigrationMessage);

        publisher.publish(topic.getQualifiedName(), beforeMigrationMessage.body());
        wait.untilConsumerCommitsOffset();

        Topic migratedTopic = topic()
                .applyPatch(topic)
                .withContentType(AVRO)
                .withMessageSchema(user.getSchemaAsString())
                .migratedFromJsonType()
                .build();
        operations.updateTopic("migrated", "topic", migratedTopic);

        // when
        Response response = publisher.publish(topic.getQualifiedName(), afterMigrationMessage.withEmptyAvroMetadata().body());

        // then
        assertThat(response).hasStatus(CREATED);
        remoteService.waitUntilReceived();
    }

    @Test
    public void shouldPublishAvroAndConsumeJsonMessageWithTraceId() throws IOException{

        // given
        byte[] avroMessage = new AvroUser("John Doe", 44, "black").asBytes();
        String jsonMessage = "{\"name\":\"John Doe\",\"age\":44,\"favoriteColor\":\"black\"}";
        String traceId = UUID.randomUUID().toString();

        // and
        Topic topic = operations.buildTopic(topic()
                .withName("publishAvroConsumeJsonWithTraceId.topic")
                .withValidation(true)
                .withMessageSchema(user.getSchemaAsString())
                .withContentType(AVRO).build()
        );
        operations.createSubscription(topic, "subscription", HTTP_ENDPOINT_URL);
        remoteService.expectMessages(jsonMessage);
        WebTarget client = ClientBuilder.newClient().target(FRONTEND_URL).path("topics").path(topic.getQualifiedName());

        // when
        Response response = client
                .request()
                .header("Trace-Id", traceId)
                .post(Entity.entity(avroMessage, MediaType.valueOf("avro/binary")));

        // then
        assertThat(response).hasStatus(Response.Status.CREATED);
        assertThat(remoteService.waitAndGetLastRequest()).hasHeaderValue("Trace-Id", traceId);
    }
}
