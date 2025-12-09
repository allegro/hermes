package pl.allegro.tech.hermes.test.helper.assertions;

import java.util.List;
import java.util.Optional;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;
import pl.allegro.tech.hermes.api.SearchItem;
import pl.allegro.tech.hermes.api.SearchResults;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.SubscriptionSearchItem;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.api.TopicSearchItem;

@SuppressWarnings("UnusedReturnValue")
public final class SearchResultsAssertion
    extends AbstractAssert<SearchResultsAssertion, SearchResults> {
  private SearchResultsAssertion(SearchResults actual) {
    super(actual, SearchResultsAssertion.class);
  }

  public SearchResultsAssertion hasNoResults() {
    Assertions.assertThat(actual.results()).isEmpty();
    Assertions.assertThat(actual.totalCount()).isEqualTo(0);
    return this;
  }

  public SearchResultsAssertion hasExactNumberOfResults(int expectedSize) {
    Assertions.assertThat(actual.results()).hasSize(expectedSize);
    hasTotalCount(expectedSize);
    return this;
  }

  public SearchResultsAssertion hasTotalCount(long expectedTotalCount) {
    Assertions.assertThat(actual.totalCount()).isEqualTo(expectedTotalCount);
    return this;
  }

  public SearchResultsAssertion containsOnlySingleItemForTopic(Topic expectedTopic) {
    hasExactNumberOfResults(1);
    return containsItemForTopic(expectedTopic);
  }

  public SearchResultsAssertion containsItemForTopic(Topic expectedTopic) {
    Assertions.assertThat(actual.results())
        .satisfiesOnlyOnce(
            item -> {
              Assertions.assertThat(item).isInstanceOf(TopicSearchItem.class);
              TopicSearchItemAssertion.assertThat((TopicSearchItem) item)
                  .hasPropertiesOfTopic(expectedTopic);
            });
    return this;
  }

  public SearchResultsAssertion containsOnlySingleItemForSubscription(
      Subscription expectedSubscription) {
    hasExactNumberOfResults(1);
    return containsItemForSubscription(expectedSubscription);
  }

  public SearchResultsAssertion containsItemForSubscription(Subscription expectedSubscription) {
    Assertions.assertThat(actual.results())
        .satisfiesOnlyOnce(
            item -> {
              Assertions.assertThat(item).isInstanceOf(SubscriptionSearchItem.class);
              SubscriptionSearchItemAssertion.assertThat((SubscriptionSearchItem) item)
                  .hasPropertiesOfSubscription(expectedSubscription);
            });
    return this;
  }

  public TopicSearchItemAssertion containsTopicItemWithName(String expectedQualifiedName) {
    SearchItem item = getItemByName(expectedQualifiedName);
    Assertions.assertThat(item).isInstanceOf(TopicSearchItem.class);
    return TopicSearchItemAssertion.assertThat((TopicSearchItem) item);
  }

  public SubscriptionSearchItemAssertion containsSubscriptionItemWithName(
      String expectedQualifiedName) {
    SearchItem item = getItemByName(expectedQualifiedName);
    Assertions.assertThat(item).isInstanceOf(SubscriptionSearchItem.class);
    return SubscriptionSearchItemAssertion.assertThat((SubscriptionSearchItem) item);
  }

  public SearchResultsAssertion containsExactlyByNameInAnyOrder(String... expectedNames) {
    List<String> actualNames = actual.results().stream().map(SearchItem::name).toList();
    Assertions.assertThat(actualNames).containsExactlyInAnyOrder(expectedNames);
    return this;
  }

  public SearchResultsAssertion doesNotContainItemWithName(String expectedQualifiedName) {
    Assertions.assertThat(actual.results())
        .noneMatch(item -> item.name().equals(expectedQualifiedName));
    return this;
  }

  public SearchResultsAssertion containsItemWithName(String expectedQualifiedName) {
    Assertions.assertThat(actual.results())
        .anyMatch(item -> item.name().equals(expectedQualifiedName));
    return this;
  }

  public SearchResultsAssertion containsExactlyByNameInAnyOrder(List<String> expectedNames) {
    return this.containsExactlyByNameInAnyOrder(expectedNames.toArray(new String[0]));
  }

  public SearchItem getItemByName(String name) {
    Optional<SearchItem> searchItem =
        actual.results().stream().filter(item -> item.name().equals(name)).findFirst();
    Assertions.assertThat(searchItem).isPresent();
    return searchItem.get();
  }

  public static SearchResultsAssertion assertThat(SearchResults actual) {
    return new SearchResultsAssertion(actual);
  }
}
