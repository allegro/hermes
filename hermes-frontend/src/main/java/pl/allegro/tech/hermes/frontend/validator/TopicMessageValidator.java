package pl.allegro.tech.hermes.frontend.validator;

import pl.allegro.tech.hermes.api.Topic;

public interface TopicMessageValidator {
    void check(byte[] message, Topic topic);
}
