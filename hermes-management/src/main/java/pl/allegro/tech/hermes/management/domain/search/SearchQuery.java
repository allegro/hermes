package pl.allegro.tech.hermes.management.domain.search;

public record SearchQuery(String query) {
  public boolean isQueryNullOrBlank() {
    return query == null || query.isBlank();
  }
}
