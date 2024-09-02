package pl.allegro.tech.hermes.domain.filtering.header;

import java.util.function.Predicate;
import java.util.regex.Pattern;
import pl.allegro.tech.hermes.api.MessageFilterSpecification;
import pl.allegro.tech.hermes.domain.filtering.FilterableMessage;
import pl.allegro.tech.hermes.domain.filtering.SubscriptionMessageFilterCompiler;

public class HeaderSubscriptionMessageFilterCompiler implements SubscriptionMessageFilterCompiler {

  @Override
  public String getType() {
    return "header";
  }

  @Override
  public Predicate<FilterableMessage> compile(MessageFilterSpecification specification) {
    return new HeaderPredicate(
        specification.getHeader(), Pattern.compile(specification.getMatcher()));
  }
}
