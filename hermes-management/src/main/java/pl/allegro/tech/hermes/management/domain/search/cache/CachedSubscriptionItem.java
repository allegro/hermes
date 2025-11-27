package pl.allegro.tech.hermes.management.domain.search.cache;

public record CachedSubscriptionItem(
    String name,
    String owner,
    String endpoint,
    String topicName
) implements CachedItem {
  @Override
  public CachedItemType getType() {
    return CachedItemType.SUBSCRIPTION;
  }

  @Override
  public String getName() {
    return name;
  }
}
