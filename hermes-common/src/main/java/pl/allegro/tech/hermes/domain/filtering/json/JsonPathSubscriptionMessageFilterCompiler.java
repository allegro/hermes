package pl.allegro.tech.hermes.domain.filtering.json;

import static com.jayway.jsonpath.Configuration.defaultConfiguration;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.Option;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import pl.allegro.tech.hermes.api.MessageFilterSpecification;
import pl.allegro.tech.hermes.domain.filtering.FilterableMessage;
import pl.allegro.tech.hermes.domain.filtering.MatchingStrategy;
import pl.allegro.tech.hermes.domain.filtering.SubscriptionMessageFilterCompiler;

public class JsonPathSubscriptionMessageFilterCompiler
    implements SubscriptionMessageFilterCompiler {
  private final Configuration configuration =
      defaultConfiguration().addOptions(Option.ALWAYS_RETURN_LIST, Option.SUPPRESS_EXCEPTIONS);

  @Override
  public String getType() {
    return "jsonpath";
  }

  @Override
  public Predicate<FilterableMessage> compile(MessageFilterSpecification specification) {
    return new JsonPathPredicate(
        specification.getPath(),
        Pattern.compile(specification.getMatcher()),
        configuration,
        MatchingStrategy.fromString(specification.getMatchingStrategy(), MatchingStrategy.ALL));
  }
}
