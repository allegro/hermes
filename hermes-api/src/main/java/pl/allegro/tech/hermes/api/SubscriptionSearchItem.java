package pl.allegro.tech.hermes.api;

public record SubscriptionSearchItem(String name, Subscription subscription) implements SearchItem {
  @Override
  public SearchItemType type() {
    return SearchItemType.SUBSCRIPTION;
  }

  public record Subscription(String endpoint, Topic topic) {}

  public record Topic(String name, String qualifiedName, String groupName) {}
}
