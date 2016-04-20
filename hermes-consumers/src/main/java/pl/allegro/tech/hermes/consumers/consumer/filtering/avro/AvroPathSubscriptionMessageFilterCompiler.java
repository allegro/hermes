package pl.allegro.tech.hermes.consumers.consumer.filtering.avro;

import pl.allegro.tech.hermes.api.MessageFilterSpecification;
import pl.allegro.tech.hermes.consumers.consumer.Message;
import pl.allegro.tech.hermes.consumers.consumer.filtering.SubscriptionMessageFilterCompiler;
import wandou.avpath.Parser;

import java.util.function.Predicate;
import java.util.regex.Pattern;

public class AvroPathSubscriptionMessageFilterCompiler implements SubscriptionMessageFilterCompiler {

    @Override
    public String getType() {
        return "avropath";
    }

    @Override
    public Predicate<Message> compile(MessageFilterSpecification specification) {
        return new AvroPathPredicate(new Parser().parse(specification.getPath()),
                Pattern.compile(specification.getMatcher()));
    }
}
