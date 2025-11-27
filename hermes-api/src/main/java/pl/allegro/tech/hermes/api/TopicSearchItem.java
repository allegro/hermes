package pl.allegro.tech.hermes.api;

public record TopicSearchItem(
    String type,
    String name
) implements SearchItem {}
