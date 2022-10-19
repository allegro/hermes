package pl.allegro.tech.hermes.domain.filtering;

import pl.allegro.tech.hermes.api.MessageFilterSpecification;

import java.util.function.Predicate;

public interface SubscriptionMessageFilterCompiler {
    String getType();

    Predicate<FilterableMessage> compile(MessageFilterSpecification specification);

    default MessageFilter getMessageFilter(MessageFilterSpecification specification) {
        return new MessageFilter(getType(), compile(specification));
    }
}
