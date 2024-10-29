package pl.allegro.tech.hermes.tracker.consumers;

import static pl.allegro.tech.hermes.api.SentMessageTraceStatus.DISCARDED;
import static pl.allegro.tech.hermes.api.SentMessageTraceStatus.INFLIGHT;
import static pl.allegro.tech.hermes.api.SentMessageTraceStatus.SUCCESS;

import org.junit.Before;
import org.junit.Test;
import pl.allegro.tech.hermes.api.SentMessageTraceStatus;

public abstract class AbstractLogRepositoryTest {

  private static final String SUBSCRIPTION = "subscription";

  private LogRepository logRepository;

  @Before
  public void setUp() {
    logRepository = createLogRepository();
  }

  protected abstract LogRepository createLogRepository();

  @Test
  public void shouldLogSentMessage() throws Exception {
    // given
    String id = "sentMessage";
    String topic = "group.sentMessage";

    // when
    logRepository.logSuccessful(TestMessageMetadata.of(id, topic, SUBSCRIPTION), "host", 1234L);

    // then
    awaitUntilMessageIsPersisted(topic, SUBSCRIPTION, id, SUCCESS);
  }

  @Test
  public void shouldLogInflightMessage() throws Exception {
    // given
    String id = "inflightMessage";
    String topic = "group.inflightMessage";

    // when
    logRepository.logInflight(TestMessageMetadata.of(id, topic, SUBSCRIPTION), 1234L);

    // then
    awaitUntilMessageIsPersisted(topic, SUBSCRIPTION, id, INFLIGHT);
  }

  @Test
  public void shouldLogUndeliveredMessage() throws Exception {
    // given
    String id = "undeliveredMessage";
    String topic = "group.undeliveredMessage";

    // when
    logRepository.logDiscarded(TestMessageMetadata.of(id, topic, SUBSCRIPTION), 1234L, "reason");

    // then
    awaitUntilMessageIsPersisted(topic, SUBSCRIPTION, id, DISCARDED);
  }

  @Test
  public void shouldLogBatchIdInSentMessage() throws Exception {
    // given
    String messageId = "messageId";
    String batchId = "batchId";
    String topic = "group.sentBatchMessage";

    // when
    logRepository.logSuccessful(
        TestMessageMetadata.of(messageId, batchId, topic, SUBSCRIPTION), "host", 1234L);

    // then
    awaitUntilBatchMessageIsPersisted(topic, SUBSCRIPTION, messageId, batchId, SUCCESS);
  }

  protected abstract void awaitUntilMessageIsPersisted(
      String topic, String subscription, String id, SentMessageTraceStatus status) throws Exception;

  protected abstract void awaitUntilBatchMessageIsPersisted(
      String topic,
      String subscription,
      String messageId,
      String batchId,
      SentMessageTraceStatus status)
      throws Exception;
}
