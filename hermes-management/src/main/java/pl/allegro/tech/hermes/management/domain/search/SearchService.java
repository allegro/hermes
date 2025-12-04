package pl.allegro.tech.hermes.management.domain.search;

import java.util.function.Predicate;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.allegro.tech.hermes.api.SearchItem;
import pl.allegro.tech.hermes.api.SearchResults;
import pl.allegro.tech.hermes.api.SubscriptionSearchItem;
import pl.allegro.tech.hermes.api.TopicSearchItem;
import pl.allegro.tech.hermes.management.domain.search.cache.CachedItem;
import pl.allegro.tech.hermes.management.domain.search.cache.CachedSubscriptionItem;
import pl.allegro.tech.hermes.management.domain.search.cache.CachedTopicItem;
import pl.allegro.tech.hermes.management.domain.search.cache.SearchCache;

@Component
public class SearchService {
  private final SearchCache searchCache;
  private final SearchPredicateFactory searchPredicateFactory;

  @Autowired
  public SearchService(SearchCache searchCache, SearchPredicateFactory searchPredicateFactory) {
    this.searchCache = searchCache;
    this.searchPredicateFactory = searchPredicateFactory;
  }

  public SearchResults search(SearchQuery query) {
    if (query.isQueryNullOrBlank()) {
      return SearchResults.empty();
    }

    Stream<CachedItem> cachedItems = searchCache.getAllItems();
    Stream<CachedItem> filteredItems = filterItemsMatchingQuery(cachedItems, query);

    return toSearchResults(filteredItems);
  }

  private Stream<CachedItem> filterItemsMatchingQuery(Stream<CachedItem> items, SearchQuery query) {
    Predicate<CachedItem> predicate = searchPredicateFactory.buildPredicate(query);

    return items.filter(predicate);
  }

  private SearchResults toSearchResults(Stream<CachedItem> cachedItems) {
    var items = cachedItems.map(this::toSearchItem).toList();

    return new SearchResults(items, items.size());
  }

  private SearchItem toSearchItem(CachedItem cachedItem) {
    return switch (cachedItem) {
      case CachedTopicItem item ->
          new TopicSearchItem(
              item.name(),
              new TopicSearchItem.Topic(item.groupName(), new TopicSearchItem.Owner(item.owner())));
      case CachedSubscriptionItem item ->
          new SubscriptionSearchItem(
              item.name(),
              new SubscriptionSearchItem.Subscription(
                  item.endpoint(),
                  new SubscriptionSearchItem.Topic(
                      item.topicName(), item.topicQualifiedName(), item.groupName())));
    };
  }
}
