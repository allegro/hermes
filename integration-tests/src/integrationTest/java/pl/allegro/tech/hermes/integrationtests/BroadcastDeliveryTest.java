package pl.allegro.tech.hermes.integrationtests;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.waitAtMost;
import static pl.allegro.tech.hermes.api.SubscriptionPolicy.Builder.subscriptionPolicy;
import static pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder.subscription;
import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.topicWithRandomName;

import java.time.Duration;
import java.util.List;
import java.util.stream.Stream;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import pl.allegro.tech.hermes.api.ContentType;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.SubscriptionMetrics;
import pl.allegro.tech.hermes.api.SubscriptionMode;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.integrationtests.setup.HermesExtension;
import pl.allegro.tech.hermes.integrationtests.subscriber.TestSubscriber;
import pl.allegro.tech.hermes.integrationtests.subscriber.TestSubscribersExtension;
import pl.allegro.tech.hermes.test.helper.message.TestMessage;

public class BroadcastDeliveryTest {

  @RegisterExtension public static final HermesExtension hermes = new HermesExtension();

  @RegisterExtension
  public static final TestSubscribersExtension subscribersFactory = new TestSubscribersExtension();

  @Test
  public void shouldPublishAndConsumeMessageByAllServices() {
    // given
    Topic topic = hermes.initHelper().createTopic(topicWithRandomName().build());
    TestMessage message = TestMessage.random();

    List<TestSubscriber> subscribers = succeedingSubscribers(4);
    String endpointUrl = setUpSubscribersAndGetEndpoint(subscribers);

    hermes
        .initHelper()
        .createSubscription(broadcastSubscription(topic, "subscription", endpointUrl));

    // when
    hermes.api().publishUntilSuccess(topic.getQualifiedName(), message.body());

    // then
    subscribers.forEach(s -> s.waitUntilReceived(message.body()));
  }

  @Test
  public void shouldPublishAndRetryOnlyForUndeliveredConsumers() {
    // given
    Topic topic = hermes.initHelper().createTopic(topicWithRandomName().build());
    TestMessage message = TestMessage.random();

    List<TestSubscriber> subscribers = succeedingSubscribers(3);
    TestSubscriber retryingSubscriber =
        subscribersFactory.createSubscriberWithRetry(message.body(), 1);
    subscribers.add(retryingSubscriber);

    String endpointUrl = setUpSubscribersAndGetEndpoint(subscribers);

    hermes
        .initHelper()
        .createSubscription(broadcastSubscription(topic, "subscription", endpointUrl));

    // when
    hermes.api().publishUntilSuccess(topic.getQualifiedName(), message.body());

    // then
    subscribers.forEach(s -> s.waitUntilReceived(message.body()));
    retryingSubscriber.waitUntilReceived(Duration.ofMinutes(1), 2);
    Assertions.assertThat(
            retryingSubscriber.getLastReceivedRequest().getHeader("Hermes-Retry-Count"))
        .isEqualTo("1");
  }

  @Test
  public void shouldNotRetryForBadRequestsFromConsumers() {
    // given
    Topic topic = hermes.initHelper().createTopic(topicWithRandomName().build());
    TestMessage message = TestMessage.random();

    List<TestSubscriber> subscribers = succeedingSubscribers(3);
    subscribers.add(subscribersFactory.createSubscriber(400));

    String endpointUrl = setUpSubscribersAndGetEndpoint(subscribers);

    hermes
        .initHelper()
        .createSubscription(broadcastSubscription(topic, "subscription", endpointUrl));

    // when
    hermes.api().publishUntilSuccess(topic.getQualifiedName(), message.body());

    // then
    subscribers.forEach(s -> s.waitUntilReceived(message.body()));
    waitAtMost(Duration.ofSeconds(10))
        .untilAsserted(
            () -> {
              long discarded =
                  hermes
                      .api()
                      .getSubscriptionMetrics(topic.getQualifiedName(), "subscription")
                      .expectBody(SubscriptionMetrics.class)
                      .returnResult()
                      .getResponseBody()
                      .getDiscarded();
              assertThat(discarded).isEqualTo(1);
            });
  }

  private List<TestSubscriber> succeedingSubscribers(int subscribersCount) {
    return Stream.generate(subscribersFactory::createSubscriber)
        .limit(subscribersCount)
        .collect(toList());
  }

  private Subscription broadcastSubscription(
      Topic topic, String subscriptionName, String endpoint) {
    return subscription(topic, subscriptionName)
        .withEndpoint(endpoint)
        .withContentType(ContentType.JSON)
        .withSubscriptionPolicy(subscriptionPolicy().applyDefaults().build())
        .withMode(SubscriptionMode.BROADCAST)
        .build();
  }

  private String setUpSubscribersAndGetEndpoint(List<TestSubscriber> subscribers) {
    return subscribers.stream().map(TestSubscriber::getEndpoint).collect(joining(";"));
  }
}
