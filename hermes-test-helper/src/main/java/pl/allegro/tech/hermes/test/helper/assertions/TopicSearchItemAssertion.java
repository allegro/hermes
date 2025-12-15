package pl.allegro.tech.hermes.test.helper.assertions;

import org.assertj.core.api.Assertions;
import pl.allegro.tech.hermes.api.SearchItemType;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.api.TopicSearchItem;

@SuppressWarnings("UnusedReturnValue")
public final class TopicSearchItemAssertion
    extends SearchItemAssertion<TopicSearchItemAssertion, TopicSearchItem> {
  private TopicSearchItemAssertion(TopicSearchItem actual) {
    super(actual, TopicSearchItemAssertion.class);
  }

  public TopicSearchItemAssertion hasPropertiesOfTopic(Topic expectedTopic) {
    return hasName(expectedTopic.getQualifiedName())
        .hasType(SearchItemType.TOPIC)
        .hasGroupName(expectedTopic.getName().getGroupName())
        .hasOwnerId(expectedTopic.getOwner().getId());
  }

  public TopicSearchItemAssertion hasGroupName(String expected) {
    Assertions.assertThat(actual.topic().groupName()).isEqualTo(expected);
    return this;
  }

  public TopicSearchItemAssertion hasOwnerId(String expected) {
    Assertions.assertThat(actual.topic().owner().id()).isEqualTo(expected);
    return this;
  }

  public static TopicSearchItemAssertion assertThat(TopicSearchItem actual) {
    return new TopicSearchItemAssertion(actual);
  }
}
