package pl.allegro.tech.hermes.integrationtests;

import static java.util.stream.IntStream.range;
import static org.assertj.core.api.Assertions.assertThat;
import static pl.allegro.tech.hermes.api.ContentType.AVRO;
import static pl.allegro.tech.hermes.api.TopicWithSchema.topicWithSchema;
import static pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder.subscriptionWithRandomName;
import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.topicWithRandomName;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.api.TopicWithSchema;
import pl.allegro.tech.hermes.integrationtests.setup.HermesExtension;
import pl.allegro.tech.hermes.integrationtests.subscriber.TestSubscriber;
import pl.allegro.tech.hermes.integrationtests.subscriber.TestSubscribersExtension;
import pl.allegro.tech.hermes.test.helper.avro.AvroUser;
import pl.allegro.tech.hermes.test.helper.message.TestMessage;

public class KafkaSingleMessageReaderTest {

  private static final int NUMBER_OF_PARTITIONS = 2;

  private static final String PRIMARY_KAFKA_CLUSTER_NAME = "primary-dc";

  private final AvroUser avroUser = new AvroUser("Bob", 50, "blue");

  private final List<String> messages =
      new ArrayList<>() {
        {
          range(0, 3).forEach(i -> add(TestMessage.random().body()));
        }
      };

  @RegisterExtension public static final HermesExtension hermes = new HermesExtension();

  @RegisterExtension
  public static final TestSubscribersExtension subscribers = new TestSubscribersExtension();

  @Test
  public void shouldFetchSingleMessageByTopicPartitionAndOffset() {
    // given
    TestSubscriber subscriber = subscribers.createSubscriber();
    Topic topic = hermes.initHelper().createTopic(topicWithRandomName().build());
    hermes
        .initHelper()
        .createSubscription(
            subscriptionWithRandomName(topic.getName(), subscriber.getEndpoint()).build());
    messages.forEach(message -> hermes.api().publish(topic.getQualifiedName(), message));
    messages.forEach(subscriber::waitUntilReceived);

    // when
    List<String> previews = fetchPreviewsFromAllPartitions(topic.getQualifiedName(), 10, true);

    // then
    assertThat(previews).containsAll(messages);
  }

  @Test
  public void shouldFetchSingleAvroMessage() {
    // given
    TestSubscriber subscriber = subscribers.createSubscriber();
    TopicWithSchema topic =
        topicWithSchema(
            topicWithRandomName().withContentType(AVRO).build(), avroUser.getSchemaAsString());
    hermes.initHelper().createTopicWithSchema(topic);
    hermes
        .initHelper()
        .createSubscription(
            subscriptionWithRandomName(topic.getName(), subscriber.getEndpoint()).build());

    hermes.api().publishAvro(topic.getQualifiedName(), avroUser.asBytes());
    subscriber.waitUntilAnyMessageReceived();

    // when
    List<String> previews = fetchPreviewsFromAllPartitions(topic.getQualifiedName(), 10, false);

    // then
    boolean isMessagePresent =
        previews.stream().anyMatch(preview -> preview.contains(avroUser.getName()));
    assertThat(isMessagePresent).isTrue();
  }

  @Test
  public void shouldFetchSingleAvroMessageWithSchemaAwareSerialization() {
    // given
    TestSubscriber subscriber = subscribers.createSubscriber();
    TopicWithSchema topic =
        topicWithSchema(
            topicWithRandomName().withContentType(AVRO).withSchemaIdAwareSerialization().build(),
            avroUser.getSchemaAsString());
    hermes.initHelper().createTopicWithSchema(topic);
    hermes
        .initHelper()
        .createSubscription(
            subscriptionWithRandomName(topic.getName(), subscriber.getEndpoint()).build());

    hermes.api().publishAvro(topic.getQualifiedName(), avroUser.asBytes());
    subscriber.waitUntilAnyMessageReceived();

    // when
    List<String> previews = fetchPreviewsFromAllPartitions(topic.getQualifiedName(), 10, false);

    // then
    boolean isMessagePresent =
        previews.stream().anyMatch(preview -> preview.contains(avroUser.getName()));
    assertThat(isMessagePresent).isTrue();
  }

  @Test
  public void shouldReturnNotFoundErrorForNonExistingOffset() {
    // given
    final long nonExistingOffset = 10L;
    TestSubscriber subscriber = subscribers.createSubscriber();
    Topic topic = hermes.initHelper().createTopic(topicWithRandomName().build());
    hermes
        .initHelper()
        .createSubscription(
            subscriptionWithRandomName(topic.getName(), subscriber.getEndpoint()).build());
    messages.forEach(message -> hermes.api().publish(topic.getQualifiedName(), message));
    messages.forEach(subscriber::waitUntilReceived);

    // when
    WebTestClient.ResponseSpec response =
        hermes
            .api()
            .getPreview(topic.getQualifiedName(), PRIMARY_KAFKA_CLUSTER_NAME, 0, nonExistingOffset);

    // then
    response.expectStatus().isNotFound();
  }

  @Test
  public void shouldReturnNotFoundErrorForNonExistingPartition() {
    // given
    int nonExistingPartition = 20;
    Topic topic = hermes.initHelper().createTopic(topicWithRandomName().build());

    // when
    WebTestClient.ResponseSpec response =
        hermes
            .api()
            .getPreview(
                topic.getQualifiedName(), PRIMARY_KAFKA_CLUSTER_NAME, nonExistingPartition, 0L);

    // then
    response.expectStatus().isNotFound();
  }

  private List<String> fetchPreviewsFromAllPartitions(
      String qualifiedTopicName, int upToOffset, boolean unwrap) {
    List<String> result = new ArrayList<>();
    for (int partition = 0; partition < NUMBER_OF_PARTITIONS; partition++) {
      long offset = 0;
      while (offset <= upToOffset) {
        try {
          String wrappedMessage =
              hermes
                  .api()
                  .getPreview(qualifiedTopicName, PRIMARY_KAFKA_CLUSTER_NAME, partition, offset)
                  .expectBody(String.class)
                  .returnResult()
                  .getResponseBody();
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
