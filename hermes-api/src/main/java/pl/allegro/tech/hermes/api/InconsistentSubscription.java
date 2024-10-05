package pl.allegro.tech.hermes.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class InconsistentSubscription {
  private final String name;
  private final List<InconsistentMetadata> inconsistentMetadata;

  @JsonCreator
  public InconsistentSubscription(
      @JsonProperty("name") String name,
      @JsonProperty("inconsistentMetadata") List<InconsistentMetadata> inconsistentMetadata) {
    this.name = name;
    this.inconsistentMetadata = inconsistentMetadata;
  }

  public String getName() {
    return name;
  }

  public List<InconsistentMetadata> getInconsistentMetadata() {
    return inconsistentMetadata;
  }
}
