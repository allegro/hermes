package pl.allegro.tech.hermes.tracker.management;

import java.util.List;
import pl.allegro.tech.hermes.api.MessageTrace;
import pl.allegro.tech.hermes.api.SentMessageTrace;

public interface LogRepository {

  List<SentMessageTrace> getLastUndeliveredMessages(
      String topicName, String subscriptionName, int limit);

  List<MessageTrace> getMessageStatus(
      String qualifiedTopicName, String subscriptionName, String messageId);
}
