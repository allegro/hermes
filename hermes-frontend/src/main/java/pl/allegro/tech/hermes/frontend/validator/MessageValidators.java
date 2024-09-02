package pl.allegro.tech.hermes.frontend.validator;

import java.util.List;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.frontend.publishing.message.Message;

public class MessageValidators {

  private final List<TopicMessageValidator> messageValidators;

  public MessageValidators(List<TopicMessageValidator> messageValidators) {
    this.messageValidators = messageValidators;
  }

  public void check(Topic topic, Message message) {
    messageValidators.forEach(v -> v.check(message, topic));
  }
}
