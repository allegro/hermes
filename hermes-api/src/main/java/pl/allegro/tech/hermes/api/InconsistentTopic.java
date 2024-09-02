package pl.allegro.tech.hermes.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class InconsistentTopic {
  private final String name;
  private final List<InconsistentMetadata> inconsistentMetadata;
  private final List<InconsistentSubscription> inconsistentSubscriptions;

  @JsonCreator
  public InconsistentTopic(
      @JsonProperty("name") String name,
      @JsonProperty("inconsistentMetadata") List<InconsistentMetadata> inconsistentMetadata,
      @JsonProperty("inconsistentSubscriptions")
          List<InconsistentSubscription> inconsistentSubscriptions) {
    this.name = name;
    this.inconsistentMetadata = inconsistentMetadata;
    this.inconsistentSubscriptions = inconsistentSubscriptions;
  }

  public String getName() {
    return name;
  }

  public List<InconsistentMetadata> getInconsistentMetadata() {
    return inconsistentMetadata;
  }

  public List<InconsistentSubscription> getInconsistentSubscriptions() {
    return inconsistentSubscriptions;
  }
}
