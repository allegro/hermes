package pl.allegro.tech.hermes.management.domain.search.cache;

import pl.allegro.tech.hermes.api.Topic;

public record CachedTopicItem(String name, Topic topic) implements CachedItem {
  @Override
  public CachedItemType type() {
    return CachedItemType.TOPIC;
  }

  @Override
  public String name() {
    return name;
  }
}
