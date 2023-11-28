package pl.allegro.tech.hermes.integrationtests;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.http.HttpHeaders;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.integrationtests.setup.TraceContext;
import pl.allegro.tech.hermes.integrationtests.subscriber.TestJmsSubscriber;
import pl.allegro.tech.hermes.integrationtests.setup.HermesExtension;
import pl.allegro.tech.hermes.integrationtests.setup.JmsStarter;
import pl.allegro.tech.hermes.test.helper.message.TestMessage;

import java.util.UUID;

import static pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder.subscriptionWithRandomName;
import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.topicWithRandomName;

public class JmsConsumingTest {

    private static final String JMS_TOPIC_NAME = "hermes";

    @RegisterExtension
    public static final HermesExtension hermes = new HermesExtension();

    @BeforeAll
    static void startJms() throws Exception {
        JmsStarter starter = new JmsStarter();
        starter.start();
    }

    @Test
    public void shouldConsumeMessageOnJMSEndpoint() {
        // given
        Topic topic = hermes.initHelper().createTopic(topicWithRandomName().build());
        TestJmsSubscriber subscriber = new TestJmsSubscriber(JMS_TOPIC_NAME);
        hermes.initHelper().createSubscription(subscriptionWithRandomName(topic.getName(), jmsEndpointAddress()).build());

        // when
        hermes.api().publish(topic.getQualifiedName(), TestMessage.simple().body());

        // then
        subscriber.waitUntilReceived(TestMessage.simple().body());
    }

    @Test
    public void shouldPublishAndConsumeJmsMessageWithTraceId() {
        // given
        String traceId = UUID.randomUUID().toString();
        HttpHeaders headers = new HttpHeaders();
        headers.add("Trace-Id", traceId);
        Topic topic = hermes.initHelper().createTopic(topicWithRandomName().build());
        TestJmsSubscriber subscriber = new TestJmsSubscriber(JMS_TOPIC_NAME);
        hermes.initHelper().createSubscription(subscriptionWithRandomName(topic.getName(), jmsEndpointAddress()).build());

        // when
        hermes.api().publishWithHeaders(topic.getQualifiedName(), TestMessage.simple().body(), headers);

        // then
        subscriber.waitUntilMessageWithHeaderReceived("TraceId", traceId);
    }

    @Test
    public void shouldPublishAndConsumeJmsMessageWithTraceHeaders() {
        // given
        TraceContext trace = TraceContext.random();
        HttpHeaders headers = new HttpHeaders();
        addTraceHeaders(trace, headers);
        Topic topic = hermes.initHelper().createTopic(topicWithRandomName().build());
        TestJmsSubscriber subscriber = new TestJmsSubscriber(JMS_TOPIC_NAME);
        hermes.initHelper().createSubscription(subscriptionWithRandomName(topic.getName(), jmsEndpointAddress()).build());

        // when
        hermes.api().publishWithHeaders(topic.getQualifiedName(), TestMessage.simple().body(), headers);

        // then
        subscriber.waitUntilMessageWithHeaderReceived("TraceId", trace.getTraceId());
        subscriber.waitUntilMessageWithHeaderReceived("SpanId", trace.getSpanId());
        subscriber.waitUntilMessageWithHeaderReceived("ParentSpanId", trace.getParentSpanId());
        subscriber.waitUntilMessageWithHeaderReceived("TraceSampled", trace.getTraceSampled());
        subscriber.waitUntilMessageWithHeaderReceived("TraceReported", trace.getTraceReported());
    }

    private void addTraceHeaders(TraceContext trace, HttpHeaders headers) {
        headers.add("Trace-Id", trace.getTraceId());
        headers.add("Span-Id", trace.getSpanId());
        headers.add("Parent-Span-Id", trace.getParentSpanId());
        headers.add("Trace-Sampled", trace.getTraceSampled());
        headers.add("Trace-Reported", trace.getTraceReported());
    }

    private String jmsEndpointAddress() {
        return "jms://guest:guest@localhost:5445/" + JmsConsumingTest.JMS_TOPIC_NAME;
    }
}
