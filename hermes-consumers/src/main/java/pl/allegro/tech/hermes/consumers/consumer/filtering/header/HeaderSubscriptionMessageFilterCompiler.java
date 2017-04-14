package pl.allegro.tech.hermes.consumers.consumer.filtering.header;

import pl.allegro.tech.hermes.api.MessageFilterSpecification;
import pl.allegro.tech.hermes.consumers.consumer.Message;
import pl.allegro.tech.hermes.consumers.consumer.filtering.SubscriptionMessageFilterCompiler;

import java.util.function.Predicate;
import java.util.regex.Pattern;

public class HeaderSubscriptionMessageFilterCompiler implements SubscriptionMessageFilterCompiler {

    @Override
    public String getType() {
        return "header";
    }

    @Override
    public Predicate<Message> compile(MessageFilterSpecification specification) {
        return new HeaderPredicate(specification.getHeader(), Pattern.compile(specification.getMatcher()));

    }
}
