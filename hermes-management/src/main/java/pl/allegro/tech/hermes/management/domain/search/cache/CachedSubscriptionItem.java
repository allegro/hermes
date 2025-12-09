package pl.allegro.tech.hermes.management.domain.search.cache;

import pl.allegro.tech.hermes.api.Subscription;

public record CachedSubscriptionItem(String name, Subscription subscription) implements CachedItem {
  @Override
  public CachedItemType type() {
    return CachedItemType.SUBSCRIPTION;
  }

  @Override
  public String name() {
    return name;
  }
}
