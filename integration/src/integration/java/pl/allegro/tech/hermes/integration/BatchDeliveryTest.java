package pl.allegro.tech.hermes.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import pl.allegro.tech.hermes.api.BatchSubscriptionPolicy;
import pl.allegro.tech.hermes.api.ContentType;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.integration.env.SharedServices;
import pl.allegro.tech.hermes.test.helper.avro.AvroUser;
import pl.allegro.tech.hermes.test.helper.endpoint.RemoteServiceEndpoint;
import pl.allegro.tech.hermes.test.helper.message.TestMessage;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import javax.ws.rs.core.Response;

import static java.util.Arrays.stream;
import static pl.allegro.tech.hermes.api.BatchSubscriptionPolicy.Builder.batchSubscriptionPolicy;
import static pl.allegro.tech.hermes.api.TopicWithSchema.topicWithSchema;
import static pl.allegro.tech.hermes.integration.test.HermesAssertions.assertThat;
import static pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder.subscription;
import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.topic;

public class BatchDeliveryTest extends IntegrationTest {

    private RemoteServiceEndpoint remoteService;

    private ObjectMapper mapper = new ObjectMapper();

    @BeforeMethod
    public void initializeAlways() {
        this.remoteService = new RemoteServiceEndpoint(SharedServices.services().serviceMock());
    }

    private static final TestMessage[] SMALL_BATCH = TestMessage.simpleMessages(2);

    private static final TestMessage SINGLE_MESSAGE = TestMessage.simple();

    @Test
    public void shouldDeliverMessagesInBatch() throws IOException {
        // given
        Topic topic = operations.buildTopic("batchSizeTest", "topic");
        operations.createBatchSubscription(topic, remoteService.getUrl(), buildBatchPolicy()
                .withBatchSize(2)
                .withBatchTime(Integer.MAX_VALUE)
                .withBatchVolume(1024)
                .build());

        remoteService.expectMessages(SMALL_BATCH);

        // when
        stream(SMALL_BATCH).forEach(m -> publish(topic, m));

        // then
        expectSingleBatch(SMALL_BATCH);
    }

    @Test
    public void shouldDeliverBatchInGivenTimePeriod() throws IOException {
        // given
        Topic topic = operations.buildTopic("deliverBatchInGivenTimePeriod", "topic");
        operations.createBatchSubscription(topic, remoteService.getUrl(), buildBatchPolicy()
                .withBatchSize(100)
                .withBatchTime(1)
                .withBatchVolume(1024)
                .build());

        remoteService.expectMessages(SINGLE_MESSAGE);

        // when
        publish(topic, SINGLE_MESSAGE);

        // then
        expectSingleBatch(SINGLE_MESSAGE);
    }

    @Test
    public void shouldDeliverBatchInGivenVolume() throws IOException, InterruptedException {
        // given
        Topic topic = operations.buildTopic("deliverBatchInGivenVolume", "topic");
        int batchVolumeThatFitsOneMessageOnly = 150;
        operations.createBatchSubscription(topic, remoteService.getUrl(), buildBatchPolicy()
                .withBatchSize(100)
                .withBatchTime(Integer.MAX_VALUE)
                .withBatchVolume(batchVolumeThatFitsOneMessageOnly)
                .build());

        remoteService.expectMessages(SINGLE_MESSAGE);

        // when publishing more than buffer capacity
        publish(topic, SINGLE_MESSAGE);
        publish(topic, SINGLE_MESSAGE);

        // then we expect to receive batch that has desired batch volume (one message only)
        expectSingleBatch(SINGLE_MESSAGE);
    }

    @Test
    public void shouldDeliverAvroMessagesAsJsonBatch() {
        // given
        AvroUser user = new AvroUser("Bob", 50, "blue");
        Topic topic = topic("batch.avro.topic").build();
        operations.buildTopicWithSchema(topicWithSchema(topic, user.getSchemaAsString()));

        operations.createBatchSubscription(topic, remoteService.getUrl(), buildBatchPolicy()
                .withBatchSize(2)
                .withBatchTime(Integer.MAX_VALUE)
                .withBatchVolume(1024)
                .build());

        TestMessage[] avroBatch = {user.asTestMessage(), user.asTestMessage()};
        remoteService.expectMessages(avroBatch);

        // when
        stream(avroBatch).forEach(m -> publish(topic, user.asTestMessage()));

        // then
        expectSingleBatch(avroBatch);
    }

    @Test
    public void shouldPassSubscriptionHeaders() {
        // given
        Topic topic = operations.buildTopic("deliverBatchWithSubscriptionHeaders", "topic");
        BatchSubscriptionPolicy policy = buildBatchPolicy()
                .withBatchSize(100)
                .withBatchTime(1)
                .withBatchVolume(1024)
                .build();
        Subscription subscription = subscription(topic, "batchSubscription")
                .withEndpoint(remoteService.getUrl())
                .withContentType(ContentType.JSON)
                .withSubscriptionPolicy(policy)
                .withHeader("MY-HEADER", "myHeaderValue")
                .withHeader("MY-OTHER-HEADER", "myOtherHeaderValue")
                .build();
        operations.createSubscription(topic, subscription);

        remoteService.expectMessages(SINGLE_MESSAGE);

        // when
        publish(topic, SINGLE_MESSAGE);

        // then
        remoteService.waitUntilRequestReceived(request -> {
            assertThat(request.getHeader("MY-HEADER")).isEqualTo("myHeaderValue");
            assertThat(request.getHeader("MY-OTHER-HEADER")).isEqualTo("myOtherHeaderValue");
        });
    }

    @Test
    public void shouldAttachSubscriptionIdentityHeadersWhenItIsEnabled() {
        // given
        Topic topic = operations.buildTopic("deliverBatchWithSubscriptionIdentityHeaders", "topic");
        BatchSubscriptionPolicy policy = buildBatchPolicy()
                .withBatchSize(100)
                .withBatchTime(1)
                .withBatchVolume(1024)
                .build();
        Subscription subscription = subscription(topic, "batchSubscription")
                .withEndpoint(remoteService.getUrl())
                .withContentType(ContentType.JSON)
                .withSubscriptionPolicy(policy)
                .withAttachingIdentityHeadersEnabled(true)
                .build();
        operations.createSubscription(topic, subscription);
        remoteService.expectMessages(SINGLE_MESSAGE);

        // when
        publish(topic, SINGLE_MESSAGE);

        // then
        remoteService.waitUntilRequestReceived(request -> {
            assertThat(request.getHeader("Hermes-Topic-Name")).isEqualTo("deliverBatchWithSubscriptionIdentityHeaders.topic");
            assertThat(request.getHeader("Hermes-Subscription-Name")).isEqualTo("batchSubscription");
        });
    }

    @Test
    public void shouldNotAttachSubscriptionIdentityHeadersWhenItIsDisabled() {
        // given
        Topic topic = operations.buildTopic("deliverBatchWithoutSubscriptionIdentityHeaders", "topic");
        BatchSubscriptionPolicy policy = buildBatchPolicy()
                .withBatchSize(100)
                .withBatchTime(1)
                .withBatchVolume(1024)
                .build();
        Subscription subscription = subscription(topic, "batchSubscription")
                .withEndpoint(remoteService.getUrl())
                .withContentType(ContentType.JSON)
                .withSubscriptionPolicy(policy)
                .withAttachingIdentityHeadersEnabled(false)
                .build();
        operations.createSubscription(topic, subscription);
        remoteService.expectMessages(SINGLE_MESSAGE);

        // when
        publish(topic, SINGLE_MESSAGE);

        // then
        remoteService.waitUntilRequestReceived(request -> {
            assertThat(request.getHeader("Hermes-Topic-Name")).isNull();
            assertThat(request.getHeader("Hermes-Subscription-Name")).isNull();
        });
    }

    @Test
    public void shouldTimeoutRequestAsAWhole() {
        //given
        Topic topic = operations.buildTopic("timeoutTest", "topic");
        operations.createBatchSubscription(topic, remoteService.getUrl(), buildBatchPolicy()
                .withBatchSize(1)
                .withBatchTime(1)
                .withBatchVolume(1024)
                .withRequestTimeout(1000)
                .build());

        remoteService.slowThenFastMessage(SINGLE_MESSAGE.body(), 10, 5000); // response chunk every 500ms, total 5s

        // when
        publish(topic, SINGLE_MESSAGE);

        // then
        // first request is retried because of timeout (with socket / idle timeout only, the request wouldn't be timed out)
        remoteService.waitUntilReceived(5, 2);
        assertThat(remoteService.getLastReceivedRequest().getHeader("Hermes-Retry-Count")).isEqualTo("1");
    }

    private void publish(Topic topic, TestMessage m) {
        assertThat(publisher.publish(topic.getQualifiedName(), m.body())).hasStatusFamily(Response.Status.Family.SUCCESSFUL);
    }

    private void expectSingleBatch(TestMessage... expectedContents) {
        remoteService.waitUntilReceived(message -> {
            List<Map<String, Object>> batch = readBatch(message);
            assertThat(batch).hasSize(expectedContents.length);
            for (int i = 0; i < expectedContents.length; i++) {
                assertThat(batch.get(i).get("message")).isEqualTo(expectedContents[i].getContent());
                assertThat((String) ((Map) batch.get(i).get("metadata")).get("id")).isNotEmpty();
            }
        });
    }

    private BatchSubscriptionPolicy.Builder buildBatchPolicy() {
        return batchSubscriptionPolicy()
                .applyDefaults()
                .withMessageTtl(100)
                .withRequestTimeout(100)
                .withMessageBackoff(10);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> readBatch(String message) {
        try {
            return mapper.readValue(message, List.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
