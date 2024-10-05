package pl.allegro.tech.hermes.integrationtests;

import static pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder.subscriptionWithRandomName;
import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.topicWithRandomName;

import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.http.HttpHeaders;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.integrationtests.setup.HermesExtension;
import pl.allegro.tech.hermes.integrationtests.setup.JmsStarter;
import pl.allegro.tech.hermes.integrationtests.setup.TraceContext;
import pl.allegro.tech.hermes.integrationtests.subscriber.TestJmsSubscriber;
import pl.allegro.tech.hermes.test.helper.message.TestMessage;

public class JmsConsumingTest {

  private static final String JMS_TOPIC_NAME = "hermes";

  @RegisterExtension public static final HermesExtension hermes = new HermesExtension();

  private static final JmsStarter starter = new JmsStarter();

  @BeforeAll
  static void startJms() throws Exception {
    starter.start();
  }

  @AfterAll
  static void stopJms() throws Exception {
    starter.stop();
  }

  @Test
  public void shouldConsumeMessageOnJMSEndpoint() {
    // given
    Topic topic = hermes.initHelper().createTopic(topicWithRandomName().build());
    TestJmsSubscriber subscriber = new TestJmsSubscriber(JMS_TOPIC_NAME);
    hermes
        .initHelper()
        .createSubscription(
            subscriptionWithRandomName(topic.getName(), jmsEndpointAddress()).build());

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
    hermes
        .initHelper()
        .createSubscription(
            subscriptionWithRandomName(topic.getName(), jmsEndpointAddress()).build());

    // when
    hermes.api().publish(topic.getQualifiedName(), TestMessage.simple().body(), headers);

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
    hermes
        .initHelper()
        .createSubscription(
            subscriptionWithRandomName(topic.getName(), jmsEndpointAddress()).build());

    // when
    hermes.api().publish(topic.getQualifiedName(), TestMessage.simple().body(), headers);

    // then
    subscriber.waitUntilMessageWithHeaderReceived("TraceId", trace.traceId());
    subscriber.waitUntilMessageWithHeaderReceived("SpanId", trace.spanId());
    subscriber.waitUntilMessageWithHeaderReceived("ParentSpanId", trace.parentSpanId());
    subscriber.waitUntilMessageWithHeaderReceived("TraceSampled", trace.traceSampled());
    subscriber.waitUntilMessageWithHeaderReceived("TraceReported", trace.traceReported());
  }

  private void addTraceHeaders(TraceContext trace, HttpHeaders headers) {
    headers.add("Trace-Id", trace.traceId());
    headers.add("Span-Id", trace.spanId());
    headers.add("Parent-Span-Id", trace.parentSpanId());
    headers.add("Trace-Sampled", trace.traceSampled());
    headers.add("Trace-Reported", trace.traceReported());
  }

  private String jmsEndpointAddress() {
    return "jms://guest:guest@localhost:5445/" + JmsConsumingTest.JMS_TOPIC_NAME;
  }
}
