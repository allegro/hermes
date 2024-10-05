package pl.allegro.tech.hermes.domain.filtering;

import java.util.function.Predicate;
import pl.allegro.tech.hermes.api.MessageFilterSpecification;

public interface SubscriptionMessageFilterCompiler {
  String getType();

  Predicate<FilterableMessage> compile(MessageFilterSpecification specification);

  default MessageFilter getMessageFilter(MessageFilterSpecification specification) {
    return new MessageFilter(getType(), compile(specification));
  }
}
