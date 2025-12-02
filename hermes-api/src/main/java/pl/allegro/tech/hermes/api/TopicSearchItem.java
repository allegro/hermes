package pl.allegro.tech.hermes.api;

public record TopicSearchItem(
    String type,
    String name,
    Topic topic
) implements SearchItem {
  public record Topic(
      String groupName,
      Owner owner
  ) {}

  public record Owner(
      String id
  ) {}
}
