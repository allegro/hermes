package pl.allegro.tech.hermes.domain.filtering.avro;

import java.util.function.Predicate;
import java.util.regex.Pattern;
import pl.allegro.tech.hermes.api.MessageFilterSpecification;
import pl.allegro.tech.hermes.domain.filtering.FilterableMessage;
import pl.allegro.tech.hermes.domain.filtering.MatchingStrategy;
import pl.allegro.tech.hermes.domain.filtering.SubscriptionMessageFilterCompiler;

public class AvroPathSubscriptionMessageFilterCompiler
    implements SubscriptionMessageFilterCompiler {

  @Override
  public String getType() {
    return "avropath";
  }

  @Override
  public Predicate<FilterableMessage> compile(MessageFilterSpecification specification) {
    return new AvroPathPredicate(
        specification.getPath(),
        Pattern.compile(specification.getMatcher()),
        MatchingStrategy.fromString(specification.getMatchingStrategy(), MatchingStrategy.ALL));
  }
}
