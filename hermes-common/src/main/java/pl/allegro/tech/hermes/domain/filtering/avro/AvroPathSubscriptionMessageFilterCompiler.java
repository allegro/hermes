package pl.allegro.tech.hermes.domain.filtering.avro;

import pl.allegro.tech.hermes.api.MessageFilterSpecification;
import pl.allegro.tech.hermes.domain.filtering.Filterable;
import pl.allegro.tech.hermes.domain.filtering.MatchingStrategy;
import pl.allegro.tech.hermes.domain.filtering.SubscriptionMessageFilterCompiler;

import java.util.function.Predicate;
import java.util.regex.Pattern;

public class AvroPathSubscriptionMessageFilterCompiler implements SubscriptionMessageFilterCompiler {

    @Override
    public String getType() {
        return "avropath";
    }

    @Override
    public Predicate<Filterable> compile(MessageFilterSpecification specification) {
        return new AvroPathPredicate(
            specification.getPath(),
            Pattern.compile(specification.getMatcher()),
            MatchingStrategy.fromString(specification.getMatchingStrategy(), MatchingStrategy.ALL)
        );
    }
}
