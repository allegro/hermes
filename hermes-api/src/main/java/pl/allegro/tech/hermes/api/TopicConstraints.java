package pl.allegro.tech.hermes.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;

public class TopicConstraints {

  private final TopicName topicName;
  @Valid private final Constraints constraints;

  @JsonCreator
  public TopicConstraints(
      @JsonProperty("topicName") String topicName,
      @JsonProperty("constraints") Constraints constraints) {
    this.topicName = TopicName.fromQualifiedName(topicName);
    this.constraints = constraints;
  }

  public TopicName getTopicName() {
    return topicName;
  }

  public Constraints getConstraints() {
    return constraints;
  }
}
