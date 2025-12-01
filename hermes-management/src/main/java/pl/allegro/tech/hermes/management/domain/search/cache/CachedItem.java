package pl.allegro.tech.hermes.management.domain.search.cache;

sealed public interface CachedItem permits CachedSubscriptionItem, CachedTopicItem {
  String name();

  CachedItemType type();
}
