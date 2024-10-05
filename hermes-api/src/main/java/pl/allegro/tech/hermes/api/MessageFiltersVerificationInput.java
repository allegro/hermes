package pl.allegro.tech.hermes.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public class MessageFiltersVerificationInput {
  private final List<MessageFilterSpecification> filters;

  @NotNull private final byte[] message;

  @JsonCreator
  public MessageFiltersVerificationInput(
      @JsonProperty("filters") List<MessageFilterSpecification> filters,
      @JsonProperty("message") byte[] message) {
    this.filters = filters;
    this.message = message;
  }

  public List<MessageFilterSpecification> getFilters() {
    return filters;
  }

  public byte[] getMessage() {
    return message;
  }
}
