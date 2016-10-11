package pl.allegro.tech.hermes.common.filtering.avro;

import pl.allegro.tech.hermes.api.MessageFilterSpecification;
import pl.allegro.tech.hermes.common.filtering.SubscriptionMessageFilterCompiler;
import pl.allegro.tech.hermes.common.message.MessageContent;

import java.util.function.Predicate;
import java.util.regex.Pattern;

public class AvroPathSubscriptionMessageFilterCompiler implements SubscriptionMessageFilterCompiler {

    @Override
    public String getType() {
        return "avropath";
    }

    @Override
    public Predicate<MessageContent> compile(MessageFilterSpecification specification) {
        return new AvroPathPredicate(specification.getPath(), Pattern.compile(specification.getMatcher()));
    }
}
