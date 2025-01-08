package pl.allegro.tech.hermes.integrationtests;

import static com.google.common.collect.ImmutableMap.of;
import static jakarta.ws.rs.core.MediaType.TEXT_PLAIN;
import static pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder.subscriptionWithRandomName;
import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.topicWithRandomName;
import static pl.allegro.tech.hermes.utils.Headers.createHeaders;

import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import pl.allegro.tech.hermes.api.MessageFilterSpecification;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.integrationtests.setup.HermesExtension;
import pl.allegro.tech.hermes.integrationtests.subscriber.TestSubscriber;
import pl.allegro.tech.hermes.integrationtests.subscriber.TestSubscribersExtension;
import pl.allegro.tech.hermes.test.helper.avro.AvroUser;

public class FilteringHeadersTest {

  @RegisterExtension public static final HermesExtension hermes = new HermesExtension();

  @RegisterExtension
  public static final TestSubscribersExtension subscribers = new TestSubscribersExtension();

  private static final MessageFilterSpecification TRACE_ID_HEADER_FILTER =
      new MessageFilterSpecification(
          of("type", "header", "header", "Trace-Id", "matcher", "^vte.*"));

  private static final MessageFilterSpecification SPAN_ID_HEADER_FILTER =
      new MessageFilterSpecification(
          of("type", "header", "header", "Span-Id", "matcher", ".*span$"));

  private static final AvroUser ALICE = new AvroUser("Alice", 20, "blue");
  private static final AvroUser BOB = new AvroUser("Bob", 30, "red");

  @Test
  public void shouldFilterIncomingEventsByHeaders() {
    // given
    TestSubscriber subscriber = subscribers.createSubscriber();
    Topic topic = hermes.initHelper().createTopic(topicWithRandomName().build());

    hermes
        .initHelper()
        .createSubscription(
            subscriptionWithRandomName(topic.getName(), subscriber.getEndpoint())
                .withFilter(TRACE_ID_HEADER_FILTER)
                .withFilter(SPAN_ID_HEADER_FILTER)
                .build());

    // when
    hermes
        .api()
        .publish(
            topic.getQualifiedName(),
            ALICE.asJson(),
            createHeaders(
                Map.of("Trace-Id", "vte12", "Span-Id", "my-span", "Content-Type", TEXT_PLAIN)))
        .expectStatus()
        .is2xxSuccessful();
    hermes
        .api()
        .publish(topic.getQualifiedName(), BOB.asJson(), createHeaders(Map.of("Trace-Id", "vte12")))
        .expectStatus()
        .is2xxSuccessful();
    hermes
        .api()
        .publish(
            topic.getQualifiedName(), BOB.asJson(), createHeaders(Map.of("Span-Id", "my-span")))
        .expectStatus()
        .is2xxSuccessful();
    hermes
        .api()
        .publish(
            topic.getQualifiedName(),
            BOB.asJson(),
            createHeaders(Map.of("Trace-Id", "vte12", "Span-Id", "span-1")))
        .expectStatus()
        .is2xxSuccessful();
    hermes
        .api()
        .publish(
            topic.getQualifiedName(),
            BOB.asJson(),
            createHeaders(Map.of("Trace-Id", "invalid", "Span-Id", "my-span")))
        .expectStatus()
        .is2xxSuccessful();

    // then
    subscriber.waitUntilAllReceivedStrict(Set.of(ALICE.asJson()));
  }
}
