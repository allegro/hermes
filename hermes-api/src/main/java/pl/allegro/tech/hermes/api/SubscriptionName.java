package pl.allegro.tech.hermes.api;

import static com.google.common.base.Preconditions.checkArgument;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;

public class SubscriptionName {

  private final String name;
  private final TopicName topicName;

  @JsonCreator
  public SubscriptionName(
      @JsonProperty("name") String name, @JsonProperty("topicName") TopicName topicName) {
    this.name = name;
    this.topicName = topicName;
  }

  @JsonIgnore
  public String getQualifiedName() {
    return toString();
  }

  public String getName() {
    return name;
  }

  public TopicName getTopicName() {
    return topicName;
  }

  public static SubscriptionName fromString(String string) {
    String[] tokens = string.split("\\$");
    checkArgument(
        tokens.length > 1,
        "Incorrect string format. Expected 'topic$subscription'. Found:'%s'",
        string);
    return new SubscriptionName(tokens[1], TopicName.fromQualifiedName(tokens[0]));
  }

  @Override
  public String toString() {
    return topicName.qualifiedName() + "$" + name;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SubscriptionName that = (SubscriptionName) o;
    return Objects.equals(name, that.name) && Objects.equals(topicName, that.topicName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, topicName);
  }
}
