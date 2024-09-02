package pl.allegro.tech.hermes.domain.filtering;

import java.util.function.Predicate;

public class MessageFilter implements Predicate<FilterableMessage> {
  private final String type;
  private final Predicate<FilterableMessage> predicate;

  public MessageFilter(String type, Predicate<FilterableMessage> predicate) {
    this.type = type;
    this.predicate = predicate;
  }

  @Override
  public boolean test(FilterableMessage message) {
    return predicate.test(message);
  }

  public String getType() {
    return type;
  }
}
