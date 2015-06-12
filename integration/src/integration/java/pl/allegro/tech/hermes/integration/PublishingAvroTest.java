package pl.allegro.tech.hermes.integration;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.EncoderFactory;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.util.BytesContentProvider;
import org.eclipse.jetty.client.util.OutputStreamContentProvider;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import pl.allegro.tech.hermes.integration.env.SharedServices;
import pl.allegro.tech.hermes.test.helper.endpoint.RemoteServiceEndpoint;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CREATED;
import static org.eclipse.jetty.http.HttpHeader.CONTENT_TYPE;
import static pl.allegro.tech.hermes.api.Topic.Builder.topic;
import static pl.allegro.tech.hermes.api.Topic.ContentType.AVRO;
import static pl.allegro.tech.hermes.integration.test.HermesAssertions.assertThat;

public class PublishingAvroTest extends IntegrationTest {

    private RemoteServiceEndpoint remoteService;

    private Schema schema;
    private HttpClient httpClient;

    @BeforeClass
    public void initialize() throws Exception {
        schema = new Schema.Parser().parse(this.getClass().getResourceAsStream("/schema/user.avsc"));
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
        operations.buildTopic(
            topic().withName("avro.topic").withValidation(true).withMessageSchema(schema.toString()).withContentType(AVRO).build());
        operations.createSubscription("avro", "topic", "subscription", HTTP_ENDPOINT_URL);
        remoteService.expectMessages("{\"name\":\"Bob\",\"age\":50,\"favoriteColor\":\"blue\"}");

        // when
        ContentResponse response = publishAvroData("avro.topic", createSampleUser("Bob", 50, "blue"));

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
                .withMessageSchema(schema.toString())
                .withContentType(AVRO).build());

        // when
        ContentResponse response = httpClient.POST(FRONTEND_URL + "topics/invalidAvro.topic")
                .header(CONTENT_TYPE, "avro/binary")
                .content(new BytesContentProvider("invalidMessage".getBytes()))
                .send();

        // then
        assertThat(response.getStatus()).isEqualTo(BAD_REQUEST.getStatusCode());
    }

    private ContentResponse publishAvroData(String topicName, GenericRecord record)
            throws InterruptedException, ExecutionException, TimeoutException, IOException {

        OutputStreamContentProvider outputStreamContentProvider = new OutputStreamContentProvider();
        writeRecord(record, outputStreamContentProvider.getOutputStream());

        return httpClient.POST(FRONTEND_URL + "topics/"  + topicName)
                .header(CONTENT_TYPE, "avro/binary")
                .content(outputStreamContentProvider)
            .send();
    }

    private void writeRecord(GenericRecord record, OutputStream outputStream) throws IOException {
        BinaryEncoder encoder = EncoderFactory.get().binaryEncoder(outputStream, null);
        DatumWriter<GenericRecord> writer = new GenericDatumWriter<>(schema);

        writer.write(record, encoder);
        encoder.flush();
        outputStream.close();
    }

    private GenericRecord createSampleUser(String name, int age, String favoriteColor) throws IOException {
        GenericRecord user = new GenericData.Record(schema);
        user.put("name", name);
        user.put("age", age);
        user.put("favoriteColor", favoriteColor);

        return user;
    }

}
