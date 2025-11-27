package pl.allegro.tech.hermes.api;

import static java.util.Collections.emptyList;

public record SearchResults<Item>(
    Iterable<Item> results,
    long totalCount
) {
  public static <Item> SearchResults<Item> empty() {
    return new SearchResults<>(emptyList(), 0);
  }
}
