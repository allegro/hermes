package pl.allegro.tech.hermes.message.tracker.management;

import pl.allegro.tech.hermes.api.MessageTrace;
import pl.allegro.tech.hermes.api.SentMessageTrace;

import java.util.List;

public interface LogRepository {

    List<SentMessageTrace> getLastUndeliveredMessages(String topicName, String subscriptionName, int limit);

    List<MessageTrace> getMessageStatus(String qualifiedTopicName, String subscriptionName, String messageId);
}
