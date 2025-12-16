package pl.allegro.tech.hermes.management.domain.search.cache;

import java.util.stream.Stream;

public interface SearchCache {
  Stream<CachedItem> getAllItems();

  void initialize();
}
