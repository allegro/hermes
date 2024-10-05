package pl.allegro.tech.hermes.frontend.server;

import pl.allegro.tech.hermes.api.Topic;

final class SchemaLoadingResult {

  enum Type {
    SUCCESS,
    FAILURE,
    MISSING
  }

  private final Type type;

  private final Topic topic;

  private SchemaLoadingResult(Type type, Topic topic) {
    this.type = type;
    this.topic = topic;
  }

  static SchemaLoadingResult success(Topic topic) {
    return new SchemaLoadingResult(Type.SUCCESS, topic);
  }

  static SchemaLoadingResult failure(Topic topic) {
    return new SchemaLoadingResult(Type.FAILURE, topic);
  }

  static SchemaLoadingResult missing(Topic topic) {
    return new SchemaLoadingResult(Type.MISSING, topic);
  }

  Type getType() {
    return type;
  }

  Topic getTopic() {
    return topic;
  }

  boolean isFailure() {
    return Type.FAILURE == type;
  }
}
