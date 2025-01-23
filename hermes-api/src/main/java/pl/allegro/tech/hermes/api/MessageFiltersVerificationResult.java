package pl.allegro.tech.hermes.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class MessageFiltersVerificationResult {
  private final VerificationStatus status;
  private final String errorMessage;

  public enum VerificationStatus {
    NOT_MATCHED,
    MATCHED,
    ERROR
  }

  @JsonCreator
  public MessageFiltersVerificationResult(
      @JsonProperty("status") VerificationStatus status,
      @JsonProperty("errorMessage") String errorMessage) {
    this.status = status;
    this.errorMessage = errorMessage;
  }

  public VerificationStatus getStatus() {
    return status;
  }

  public String getErrorMessage() {
    return errorMessage;
  }
}
