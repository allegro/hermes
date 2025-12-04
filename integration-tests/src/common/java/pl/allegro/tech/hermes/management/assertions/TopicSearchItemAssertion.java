package pl.allegro.tech.hermes.management.assertions;

import pl.allegro.tech.hermes.api.TopicSearchItem;

public final class TopicSearchItemAssertion
    extends SearchItemAssertion<TopicSearchItemAssertion, TopicSearchItem> {
  private TopicSearchItemAssertion(TopicSearchItem actual) {
    super(actual, TopicSearchItemAssertion.class);
  }

  public static TopicSearchItemAssertion assertThat(TopicSearchItem actual) {
    return new TopicSearchItemAssertion(actual);
  }
}
