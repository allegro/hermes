package pl.allegro.tech.hermes.management.infrastructure.tracker;

import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.MessageTrace;
import pl.allegro.tech.hermes.api.SentMessageTrace;
import pl.allegro.tech.hermes.tracker.management.LogRepository;

public class NoOperationLogRepository implements LogRepository {

  private static final String NO_LOG_MESSAGE =
      "No LogRepository implementation found, using default no-operation implementation";
  private static final Logger logger = LoggerFactory.getLogger(NoOperationLogRepository.class);

  @Override
  public List<SentMessageTrace> getLastUndeliveredMessages(
      String topicName, String subscriptionName, int limit) {
    logger.info(NO_LOG_MESSAGE);
    return Collections.emptyList();
  }

  @Override
  public List<MessageTrace> getMessageStatus(
      String qualifiedTopicName, String subscriptionName, String messageId) {
    logger.info(NO_LOG_MESSAGE);
    return Collections.emptyList();
  }
}
