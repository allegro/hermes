package pl.allegro.tech.hermes.mock;

import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.toList;
import static org.awaitility.Awaitility.await;

import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;
import org.apache.avro.Schema;
import org.awaitility.core.ConditionTimeoutException;

public class HermesMockExpect {
  private final HermesMockHelper hermesMockHelper;
  private final HermesMockQuery hermesMockQuery;
  private final int awaitSeconds;

  public HermesMockExpect(HermesMockHelper hermesMockHelper, int awaitSeconds) {
    this.hermesMockHelper = hermesMockHelper;
    this.hermesMockQuery = new HermesMockQuery(hermesMockHelper);
    this.awaitSeconds = awaitSeconds;
  }

  public void singleMessageOnTopic(String topicName) {
    messagesOnTopic(topicName, 1);
  }

  public <T> void singleJsonMessageOnTopicAs(String topicName, Class<T> clazz) {
    jsonMessagesOnTopicAs(topicName, 1, clazz);
  }

  public void singleAvroMessageOnTopic(String topicName, Schema schema) {
    avroMessagesOnTopic(topicName, 1, schema);
  }

  public void messagesOnTopic(String topicName, int count) {
    expectMessages(topicName, count);
  }

  public <T> void jsonMessagesOnTopicAs(String topicName, int count, Class<T> clazz) {
    assertMessages(count, () -> hermesMockQuery.allJsonMessagesAs(topicName, clazz));
  }

  public <T> void jsonMessagesOnTopicAs(
      String topicName, int count, Class<T> clazz, Predicate<T> predicate) {
    assertMessages(
        count, () -> hermesMockQuery.matchingJsonMessagesAs(topicName, clazz, predicate));
  }

  public void avroMessagesOnTopic(String topicName, int count, Schema schema) {
    assertMessages(count, () -> validateAvroMessages(topicName, schema));
  }

  public <T> void avroMessagesOnTopic(
      String topicName, int count, Schema schema, Class<T> clazz, Predicate<T> predicate) {
    assertMessages(count, () -> validateAvroMessages(topicName, schema, clazz, predicate));
  }

  private <T> void assertMessages(int count, Supplier<List<T>> messages) {
    try {
      await()
          .atMost(awaitSeconds, SECONDS)
          .until(() -> (messages != null && messages.get().size() == count));
    } catch (ConditionTimeoutException ex) {
      throw new HermesMockException(
          "Hermes mock did not receive " + count + " messages, got " + messages.get().size());
    }
  }

  private void expectMessages(String topicName, int count) {
    try {
      await()
          .atMost(awaitSeconds, SECONDS)
          .untilAsserted(() -> hermesMockHelper.verifyRequest(count, topicName));
    } catch (ConditionTimeoutException ex) {
      throw new HermesMockException("Hermes mock did not receive " + count + " messages.", ex);
    }
  }

  private List<byte[]> validateAvroMessages(String topicName, Schema schema) {
    return hermesMockQuery.allAvroRawMessages(topicName).stream()
        .peek(raw -> hermesMockHelper.validateAvroSchema(raw, schema))
        .collect(toList());
  }

  private <T> List<T> validateAvroMessages(
      String topicName, Schema schema, Class<T> clazz, Predicate<T> predicate) {
    return hermesMockQuery.allAvroRawMessages(topicName).stream()
        .map(raw -> hermesMockHelper.deserializeAvro(raw, schema, clazz))
        .filter(predicate)
        .collect(toList());
  }
}
