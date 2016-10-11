package pl.allegro.tech.hermes.common.filtering;

import pl.allegro.tech.hermes.api.MessageFilterSpecification;
import pl.allegro.tech.hermes.common.message.MessageContent;

import java.util.function.Predicate;

public interface SubscriptionMessageFilterCompiler {
    String getType();
    Predicate<MessageContent> compile(MessageFilterSpecification specification);

    default MessageFilter getMessageFilter(MessageFilterSpecification specification) {
        return new MessageFilter(getType(), compile(specification));
    }
}
