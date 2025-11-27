package pl.allegro.tech.hermes.api;

public record SubscriptionSearchItem(
    String type,
    String name,
    Subscription subscription
) implements SearchItem {
  public record Subscription(
      String topicName
  ) {}
}
