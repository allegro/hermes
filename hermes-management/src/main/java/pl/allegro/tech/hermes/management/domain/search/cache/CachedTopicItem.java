package pl.allegro.tech.hermes.management.domain.search.cache;

public record CachedTopicItem(
    String name,
    String owner
) implements CachedItem {
  @Override
  public CachedItemType type() {
    return CachedItemType.TOPIC;
  }

  @Override
  public String name() {
    return name;
  }
}
