package pl.allegro.tech.hermes.integration;

import com.googlecode.catchexception.CatchException;
import net.javacrumbs.jsonunit.core.Option;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.api.TopicWithSchema;
import pl.allegro.tech.hermes.integration.env.SharedServices;
import pl.allegro.tech.hermes.integration.test.HermesAssertions;
import pl.allegro.tech.hermes.test.helper.avro.AvroUser;
import pl.allegro.tech.hermes.test.helper.endpoint.RemoteServiceEndpoint;
import pl.allegro.tech.hermes.test.helper.message.TestMessage;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.googlecode.catchexception.CatchException.catchException;
import static java.util.stream.IntStream.range;
import static javax.ws.rs.core.Response.Status.CREATED;
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;
import static pl.allegro.tech.hermes.api.ContentType.AVRO;
import static pl.allegro.tech.hermes.api.Topic.Ack.ALL;
import static pl.allegro.tech.hermes.api.TopicWithSchema.topicWithSchema;
import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.randomTopic;

public class KafkaSingleMessageReaderTest extends IntegrationTest {

    private static final int NUMBER_OF_PARTITIONS = 2;

    private RemoteServiceEndpoint remoteService;

    private final AvroUser avroUser = new AvroUser("Bob", 50, "blue");

    @BeforeMethod
    public void initializeAlways() throws Exception {
        remoteService = new RemoteServiceEndpoint(SharedServices.services().serviceMock());
    }

    @Test
    public void shouldFetchSingleMessageByTopicPartitionAndOffset() {
        // given
        Topic topic = operations.buildTopic(randomTopic("kafkaPreviewTestGroup", "topic").withAck(ALL).build());
        operations.createSubscription(topic, "subscription", HTTP_ENDPOINT_URL);

        List<String> messages = new ArrayList<String>() {{ range(0, 3).forEach(i -> add(TestMessage.random().body())); }};

        remoteService.expectMessages(messages);

        String qualifiedTopicName = topic.getQualifiedName();
        messages.forEach(message -> publisher.publish(qualifiedTopicName, message));

        remoteService.waitUntilReceived();

        // when
        List<String> previews = fetchPreviewsFromAllPartitions(qualifiedTopicName, 10, true);

        // then
        assertThat(previews).hasSize(messages.size()).contains(messages.toArray(new String[messages.size()]));
    }

    @Test
    public void shouldFetchSingleAvroMessage() throws IOException {
        // given
        Topic topic = randomTopic("avro", "fetch").withContentType(AVRO).withAck(ALL).build();
        TopicWithSchema topicWithSchema = topicWithSchema(topic, avroUser.getSchemaAsString());
        operations.buildTopicWithSchema(topicWithSchema);

        Response response = publisher.publish(topic.getQualifiedName(), avroUser.asBytes());
        HermesAssertions.assertThat(response).hasStatus(CREATED);

        // when
        List<String> previews = fetchPreviewsFromAllPartitions(topic.getQualifiedName(), 10, false);

        // then
        assertThat(previews).hasSize(1);
        assertThatJson(previews.get(0))
            .when(Option.IGNORING_EXTRA_FIELDS)
            .isEqualTo(avroUser.asJson());
    }

    @Test
    public void shouldFetchSingleAvroMessageWithSchemaAwareSerialization() throws IOException {
        // given
        Topic topic = randomTopic("avro", "fetchSchemaAwareSerialization")
                .withSchemaIdAwareSerialization()
                .withContentType(AVRO)
                .withAck(ALL)
                .build();
        TopicWithSchema topicWithSchema = topicWithSchema(topic, avroUser.getSchemaAsString());
        operations.buildTopicWithSchema(topicWithSchema);

        Response response = publisher.publish(topic.getQualifiedName(), avroUser.asBytes());
        HermesAssertions.assertThat(response).hasStatus(CREATED);

        // when
        List<String> previews = fetchPreviewsFromAllPartitions(topic.getQualifiedName(), 10, false);

        // then
        assertThat(previews).hasSize(1);
        assertThatJson(previews.get(0))
                .when(Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo(avroUser.asJson());
    }

    @Test
    public void shouldReturnNotFoundErrorForNonExistingOffset() {
        // given
        Topic topic = operations.buildTopic(randomTopic("kafkaPreviewTestGroup", "offsetTestTopic").withAck(ALL).build());
        operations.createSubscription(topic, "subscription", HTTP_ENDPOINT_URL);
        List<String> messages = new ArrayList<String>() {{ range(0, 3).forEach(i -> add(TestMessage.random().body())); }};

        remoteService.expectMessages(messages);
        messages.forEach(message -> publisher.publish(topic.getQualifiedName(), message));

        remoteService.waitUntilReceived();

        // when
        catchException(management.topic()).preview(topic.getQualifiedName(), PRIMARY_KAFKA_CLUSTER_NAME, 0, 10L);

        // then
        assertThat(CatchException.<NotFoundException>caughtException()).isInstanceOf(NotFoundException.class);
    }

    @Test
    public void shouldReturnNotFoundErrorForNonExistingPartition() {
        // given
        Topic topic = operations.buildTopic(randomTopic("kafkaPreviewTestGroup", "partitionTestTopic").withAck(ALL).build());

        // when
        catchException(management.topic()).preview(topic.getQualifiedName(), PRIMARY_KAFKA_CLUSTER_NAME, 10, 0L);

        // then
        assertThat(CatchException.<NotFoundException>caughtException()).isInstanceOf(NotFoundException.class);
    }

    private List<String> fetchPreviewsFromAllPartitions(String qualifiedTopicName, int upToOffset, boolean unwrap) {
        List<String> result = new ArrayList<>();
        for (int p = 0; p < NUMBER_OF_PARTITIONS; p++) {
            long offset = 0;
            while (offset <= upToOffset) {
                try {
                    String wrappedMessage = management.topic().preview(qualifiedTopicName, PRIMARY_KAFKA_CLUSTER_NAME, p, offset);
                    result.add(unwrap ? unwrap(wrappedMessage) : wrappedMessage);
                    offset++;
                } catch (Exception e) {
                    break;
                }
            }
        }
        return result;
    }

    private String unwrap(String wrappedMessage) {
        String msg = wrappedMessage.split("\"message\":", 2)[1];
        return msg.substring(0, msg.length() - 1);
    }

}
