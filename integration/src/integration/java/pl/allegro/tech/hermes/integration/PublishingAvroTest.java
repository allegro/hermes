package pl.allegro.tech.hermes.integration;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import pl.allegro.tech.hermes.integration.env.SharedServices;
import pl.allegro.tech.hermes.test.helper.avro.AvroUser;
import pl.allegro.tech.hermes.test.helper.endpoint.RemoteServiceEndpoint;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CREATED;
import static pl.allegro.tech.hermes.api.Topic.Builder.topic;
import static pl.allegro.tech.hermes.api.Topic.ContentType.AVRO;
import static pl.allegro.tech.hermes.integration.test.HermesAssertions.assertThat;

public class PublishingAvroTest extends IntegrationTest {

    private RemoteServiceEndpoint remoteService;

    private AvroUser user;

    @BeforeClass
    public void initialize() throws Exception {
        user = new AvroUser();
    }

    @BeforeMethod
    public void initializeAlways() {
        this.remoteService = new RemoteServiceEndpoint(SharedServices.services().serviceMock());
    }

    @Test
    public void shouldPublishAvroAndConsumeJsonMessage() throws InterruptedException, ExecutionException, TimeoutException, IOException {
        // given
        operations.buildTopic(topic()
                .withName("avro.topic")
                .withValidation(true)
                .withMessageSchema(user.getSchema().toString())
                .withContentType(AVRO).build());
        operations.createSubscription("avro", "topic", "subscription", HTTP_ENDPOINT_URL);
        remoteService.expectMessages("{\"name\":\"Bob\",\"age\":50,\"favoriteColor\":\"blue\"}");

        // when
        Response response = publisher.publish("avro.topic", user.create("Bob", 50, "blue"));

        // then
        assertThat(response.getStatus()).isEqualTo(CREATED.getStatusCode());
        remoteService.waitUntilReceived();
    }

    @Test
    public void shouldGetBadRequestForPublishingInvalidMessageWithSchema() throws InterruptedException, ExecutionException, TimeoutException {
        // given
        operations.buildTopic(topic()
                .withName("invalidAvro.topic")
                .withValidation(true)
                .withMessageSchema(user.getSchema().toString())
                .withContentType(AVRO).build());

        // when
        Response response = publisher.publish("invalidAvro.topic", "invalidMessage".getBytes());

        // then
        assertThat(response.getStatus()).isEqualTo(BAD_REQUEST.getStatusCode());
    }

}
