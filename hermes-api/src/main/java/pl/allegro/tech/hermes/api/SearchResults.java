package pl.allegro.tech.hermes.api;

import static java.util.Collections.emptyList;

import java.util.List;

public record SearchResults(List<SearchItem> results, long totalCount) {
  public static SearchResults empty() {
    return new SearchResults(emptyList(), 0);
  }
}
