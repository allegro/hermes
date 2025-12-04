package pl.allegro.tech.hermes.management.assertions;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;
import pl.allegro.tech.hermes.api.SearchItem;
import pl.allegro.tech.hermes.api.SearchResults;
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

  public SearchResultsAssertion hasTotalCount(long expectedTotalCount) {
    Assertions.assertThat(actual.totalCount()).isEqualTo(expectedTotalCount);
    return this;
  }

  public TopicSearchItemAssertion containsSingleTopicItemWithName(String expectedName) {
    Assertions.assertThat(actual.results()).hasSize(1);
    SearchItem item = actual.results().getFirst();
    Assertions.assertThat(item).isInstanceOf(TopicSearchItem.class);
    return TopicSearchItemAssertion.assertThat((TopicSearchItem) item)
        .hasName(expectedName)
        .hasType("topic");
  }

  public static SearchResultsAssertion assertThat(SearchResults actual) {
    return new SearchResultsAssertion(actual);
  }
}
