package pl.allegro.tech.hermes.consumers.consumer.filtering;

import pl.allegro.tech.hermes.api.MessageFilterSpecification;
import pl.allegro.tech.hermes.consumers.consumer.Message;

import java.util.function.Predicate;

public interface SubscriptionMessageFilterCompiler {
    String getType();
    Predicate<Message> compile(MessageFilterSpecification specification);

    default MessageFilter getMessageFilter(MessageFilterSpecification specification) {
        return new MessageFilter(getType(), compile(specification));
    }
}
