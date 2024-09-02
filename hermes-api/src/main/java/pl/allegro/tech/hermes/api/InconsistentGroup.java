package pl.allegro.tech.hermes.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class InconsistentGroup {
  private final String name;
  private final List<InconsistentMetadata> inconsistentMetadata;
  private final List<InconsistentTopic> inconsistentTopics;

  @JsonCreator
  public InconsistentGroup(
      @JsonProperty("name") String name,
      @JsonProperty("inconsistentMetadata") List<InconsistentMetadata> inconsistentMetadata,
      @JsonProperty("inconsistentTopics") List<InconsistentTopic> inconsistentTopics) {
    this.name = name;
    this.inconsistentMetadata = inconsistentMetadata;
    this.inconsistentTopics = inconsistentTopics;
  }

  public String getName() {
    return name;
  }

  public List<InconsistentMetadata> getInconsistentMetadata() {
    return inconsistentMetadata;
  }

  public List<InconsistentTopic> getInconsistentTopics() {
    return inconsistentTopics;
  }
}
