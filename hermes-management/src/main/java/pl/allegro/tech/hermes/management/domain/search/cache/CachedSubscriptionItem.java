package pl.allegro.tech.hermes.management.domain.search.cache;

public record CachedSubscriptionItem(
    String name,
    String owner,
    String endpoint,
    String topicName,
    String topicQualifiedName,
    String groupName
) implements CachedItem {
  @Override
  public CachedItemType type() {
    return CachedItemType.SUBSCRIPTION;
  }

  @Override
  public String name() {
    return name;
  }
}
