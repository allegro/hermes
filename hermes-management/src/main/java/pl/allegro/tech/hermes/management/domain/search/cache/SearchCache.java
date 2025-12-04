package pl.allegro.tech.hermes.management.domain.search.cache;

import java.util.Optional;
import java.util.stream.Stream;

public interface SearchCache {
  Optional<CachedItem> getItem(String id);

  Stream<CachedItem> getAllItems();

  void initialize();
}
