package pl.allegro.tech.hermes.metrics;

import java.util.Optional;

public class PathContext {

  private final Optional<String> group;
  private final Optional<String> topic;
  private final Optional<String> subscription;

  private PathContext(
      Optional<String> group, Optional<String> topic, Optional<String> subscription) {
    this.group = group;
    this.topic = topic;
    this.subscription = subscription;
  }

  public Optional<String> getGroup() {
    return group;
  }

  public Optional<String> getTopic() {
    return topic;
  }

  public Optional<String> getSubscription() {
    return subscription;
  }

  public static Builder pathContext() {
    return new Builder();
  }

  public static class Builder {

    private Optional<String> group = Optional.empty();
    private Optional<String> topic = Optional.empty();
    private Optional<String> subscription = Optional.empty();

    public Builder withGroup(String group) {
      this.group = Optional.of(group);
      return this;
    }

    public Builder withTopic(String topic) {
      this.topic = Optional.of(topic);
      return this;
    }

    public Builder withSubscription(String subscription) {
      this.subscription = Optional.of(subscription);
      return this;
    }

    public PathContext build() {
      return new PathContext(group, topic, subscription);
    }
  }
}
