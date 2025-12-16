package pl.allegro.tech.hermes.management.domain.search.cache;

public sealed interface CachedItem permits CachedSubscriptionItem, CachedTopicItem {

  String name();

  CachedItemType type();
}
