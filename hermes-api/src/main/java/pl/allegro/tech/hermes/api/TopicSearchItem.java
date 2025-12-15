package pl.allegro.tech.hermes.api;

public record TopicSearchItem(String name, Topic topic) implements SearchItem {
  @Override
  public SearchItemType type() {
    return SearchItemType.TOPIC;
  }

  public record Topic(String groupName, Owner owner) {}

  public record Owner(String id) {}
}
