package pl.allegro.tech.hermes.frontend.validator;

import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.frontend.publishing.message.Message;

public interface TopicMessageValidator {
    void check(Message message, Topic topic);
}
