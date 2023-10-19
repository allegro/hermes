package pl.allegro.tech.hermes.integration;

import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.integration.helper.RemoteJmsEndpoint;
import pl.allegro.tech.hermes.integration.metadata.TraceContext;
import pl.allegro.tech.hermes.test.helper.message.TestMessage;

import java.util.UUID;
import javax.jms.Message;

import static pl.allegro.tech.hermes.integration.helper.ClientBuilderHelper.createRequestWithTraceHeaders;
import static pl.allegro.tech.hermes.integration.test.HermesAssertions.assertThat;
import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.randomTopic;

public class JmsConsumingTest extends IntegrationTest {

    private RemoteJmsEndpoint jmsEndpoint;

    private static final String JMS_TOPIC_NAME = "hermes";

    @BeforeMethod
    public void initializeAlways() {
        this.jmsEndpoint = new RemoteJmsEndpoint(JMS_TOPIC_NAME);
    }

    @Test
    public void shouldConsumeMessageOnJMSEndpoint() {
        // given
        Topic topic = operations.buildTopic(randomTopic("publishJmsGroup", "topic").build());
        operations.createSubscription(topic, "subscription", jmsEndpointAddress(JMS_TOPIC_NAME));
        jmsEndpoint.expectMessages(TestMessage.of("hello", "world"));

        // when
        publisher.publish(topic.getQualifiedName(), TestMessage.of("hello", "world").body());

        // then
        jmsEndpoint.waitUntilReceived();
    }

    @Test
    public void shouldPublishAndConsumeJmsMessageWithTraceId() {

        // given
        String message = "{\"hello\": \"world\"}";
        String traceId = UUID.randomUUID().toString();

        // and
        Topic topic = operations.buildTopic(randomTopic("publishJmsGroupWithTrace", "topic").build());
        operations.createSubscription(topic, "subscription", jmsEndpointAddress(JMS_TOPIC_NAME));
        WebTarget client = ClientBuilder.newClient().target(FRONTEND_URL).path("topics").path(topic.getQualifiedName());
        jmsEndpoint.expectMessages(TestMessage.of("hello", "world"));

        // when
        Response response = client
                .request()
                .header("trace-id", traceId)
                .post(Entity.entity(message, MediaType.APPLICATION_JSON));

        // then
        assertThat(response).hasStatus(Response.Status.CREATED);
        Message lastMessage = jmsEndpoint.waitAndGetLastMessage();
        assertThat(lastMessage).assertStringProperty("traceid", traceId);
    }

    @Test
    public void shouldPublishAndConsumeJmsMessageWithTraceHeaders() {

        // given
        String message = "{\"hello\": \"world\"}";
        TraceContext trace = TraceContext.random();

        // and
        Topic topic = operations.buildTopic(randomTopic("publishJmsGroupWithTrace", "topic").build());
        operations.createSubscription(topic, "subscription", jmsEndpointAddress(JMS_TOPIC_NAME));
        jmsEndpoint.expectMessages(TestMessage.of("hello", "world"));
        Invocation.Builder request = createRequestWithTraceHeaders(FRONTEND_URL, topic.getQualifiedName(), trace);

        // when
        Response response = request.post(Entity.entity(message, MediaType.APPLICATION_JSON));

        // then
        assertThat(response).hasStatus(Response.Status.CREATED);
        Message lastMessage = jmsEndpoint.waitAndGetLastMessage();
        assertThat(lastMessage)
                .assertStringProperty("traceid", trace.getTraceId())
                .assertStringProperty("spanid", trace.getSpanId())
                .assertStringProperty("parentspanid", trace.getParentSpanId())
                .assertStringProperty("tracesampled", trace.getTraceSampled())
                .assertStringProperty("tracereported", trace.getTraceReported());
    }

    private String jmsEndpointAddress(String topicName) {
        return "jms://guest:guest@localhost:5445/" + topicName;
    }
}
