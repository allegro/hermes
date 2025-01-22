package pl.allegro.tech.hermes.common.message.undelivered;

import java.util.Optional;
import pl.allegro.tech.hermes.api.SentMessageTrace;
import pl.allegro.tech.hermes.api.TopicName;

public interface LastUndeliveredMessageReader {

  Optional<SentMessageTrace> last(TopicName topicName, String subscriptionName);
}
