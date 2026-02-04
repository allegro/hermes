package pl.allegro.tech.hermes.benchmark.environment;

import static pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder.subscription;
import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.topicWithRandomName;

import java.io.IOException;
import java.time.Duration;
import java.util.Objects;
import org.apache.commons.io.IOUtils;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.SubscriptionPolicy;
import pl.allegro.tech.hermes.api.Topic;

@State(Scope.Thread)
public class SubscriberEnvironment {

  private static final int MESSAGES_COUNT = 100_000;
  private static final int MAX_CONNECTIONS_PER_ROUTE = 200;
  private HermesPublisher publisher;
  private CountingTestSubscriber subscriber;

  @Setup(Level.Iteration)
  public void setupEnvironment(HermesEnvironment hermesEnvironment) throws Exception {
    Topic topic = hermesEnvironment.hermesHelper.createTopic(topicWithRandomName().build());
    subscriber = new CountingTestSubscriber(topic.getQualifiedName());
    publisher = createPublisher(topic, hermesEnvironment.frontendPort());
    Subscription subscription = createSubscription(hermesEnvironment, topic);
    sendMessages();
    hermesEnvironment.hermesHelper.resumeSubscription(subscription, topic);
  }

  private static HermesPublisher createPublisher(Topic topic, int port) throws IOException {
    String messageBody = loadMessageResource("completeMessage");
    return new HermesPublisher(
        MAX_CONNECTIONS_PER_ROUTE,
        "http://localhost:" + port + "/topics/" + topic.getQualifiedName(),
        messageBody);
  }

  public void waitUntilAllMessagesAreConsumed() {
    subscriber.waitUntilReceived(Duration.ofSeconds(20), MESSAGES_COUNT);
  }

  private Subscription createSubscription(HermesEnvironment hermesEnvironment, Topic topic) {
    SubscriptionPolicy subscriptionPolicy =
        new SubscriptionPolicy(10000, 100, 1000, 1000, false, 100, 5000, 0, 1, 600);
    return hermesEnvironment.hermesHelper.createSuspendedSubscription(
        subscription(topic.getQualifiedName(), "subscription", subscriber.getEndpoint())
            .withState(Subscription.State.SUSPENDED)
            .withSubscriptionPolicy(subscriptionPolicy)
            .build());
  }

  private void sendMessages() {
    for (int i = 0; i < MESSAGES_COUNT; i++) {
      int responseCode = publisher.publish();

      if (responseCode != 201) {
        throw new RuntimeException(
            "Failed to publish message, response status code: " + responseCode);
      }
    }
  }

  @TearDown(Level.Iteration)
  public void shutdownServers() {
    subscriber.stop();
  }

  @TearDown(Level.Iteration)
  public void shutdown() throws Exception {
    publisher.stop();
  }

  public static String loadMessageResource(String name) throws IOException {
    return IOUtils.toString(
        Objects.requireNonNull(
            HermesEnvironment.class.getResourceAsStream(String.format("/message/%s.json", name))));
  }
}
