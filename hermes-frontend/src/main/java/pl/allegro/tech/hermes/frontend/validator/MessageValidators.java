package pl.allegro.tech.hermes.frontend.validator;

import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.frontend.publishing.message.Message;

import javax.inject.Inject;
import java.util.List;

public class MessageValidators {

    private final List<TopicMessageValidator> messageValidators;

    @Inject
    public MessageValidators(List<TopicMessageValidator> messageValidators) {
        this.messageValidators = messageValidators;
    }

    public void check(Topic topic, Message message) {
        messageValidators.forEach(v -> v.check(message, topic));
    }

}
