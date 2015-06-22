package pl.allegro.tech.hermes.integration;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.util.BytesContentProvider;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import pl.allegro.tech.hermes.integration.env.SharedServices;
import pl.allegro.tech.hermes.test.helper.avro.AvroUser;
import pl.allegro.tech.hermes.test.helper.endpoint.RemoteServiceEndpoint;

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
    private HttpClient httpClient;

    @BeforeClass
    public void initialize() throws Exception {
        user = new AvroUser();
        httpClient = new HttpClient();
        httpClient.start();
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
        ContentResponse response = publishBytes("avro.topic", user.create("Bob", 50, "blue"));

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
        ContentResponse response = publishBytes("invalidAvro.topic", "invalidMessage".getBytes());

        // then
        assertThat(response.getStatus()).isEqualTo(BAD_REQUEST.getStatusCode());
    }

    private ContentResponse publishBytes(String topicName, byte[] data) throws InterruptedException, ExecutionException, TimeoutException {
        return httpClient.POST(FRONTEND_URL + "topics/" + topicName).content(new BytesContentProvider(data)).send();
    }

}
