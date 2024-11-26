package pl.allegro.tech.hermes.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class OfflineRetransmissionFromViewRequest extends OfflineRetransmissionRequest {

  private final String sourceViewPath;

  @JsonCreator
  public OfflineRetransmissionFromViewRequest(
      @JsonProperty("sourceViewPath") String sourceViewPath,
      @JsonProperty("targetTopic") String targetTopic) {
    super(RetransmissionType.VIEW, targetTopic);
    this.sourceViewPath = sourceViewPath;
  }

  public String getSourceViewPath() {
    return sourceViewPath;
  }

  @Override
  public String toString() {
    return "OfflineRetransmissionFromViewRequest{"
        + "sourceViewPath='"
        + sourceViewPath
        + '\''
        + ", targetTopic='"
        + getTargetTopic()
        + '\''
        + '}';
  }
}
