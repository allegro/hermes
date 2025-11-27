package pl.allegro.tech.hermes.management.domain.search;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.allegro.tech.hermes.api.SearchItem;
import pl.allegro.tech.hermes.api.SearchResults;
import pl.allegro.tech.hermes.management.domain.search.cache.CachedItem;
import pl.allegro.tech.hermes.management.domain.search.cache.SearchCache;

import java.util.stream.Stream;

@Component
public class SearchService {
  private final SearchCache searchCache;

  @Autowired
  public SearchService(SearchCache searchCache) {
    this.searchCache = searchCache;
  }

  public SearchResults<SearchItem> search(SearchQuery query) {
    if (query.isQueryNullOrBlank()) {
      return SearchResults.empty();
    }

    Stream<CachedItem> cachedItems = searchCache.getAllItems();

    return toSearchResults(cachedItems);
  }

  private SearchResults<SearchItem> toSearchResults(Stream<CachedItem> cachedItems) {
    var items = cachedItems
        .map(this::toSearchItem)
        .toList();

    return new SearchResults<>(items, items.size());
  }

  private SearchItem toSearchItem(CachedItem cachedItem) {
    return new SearchItem(
        cachedItem.getType().name(),
        cachedItem.getName()
    );
  }
}
