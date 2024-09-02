package pl.allegro.tech.hermes.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import java.util.Objects;

public final class MonitoringDetails {
  public static final MonitoringDetails EMPTY = new MonitoringDetails(Severity.NON_IMPORTANT, "");

  @NotNull private final Severity severity;

  @NotNull private final String reaction;

  @JsonCreator
  public MonitoringDetails(
      @JsonProperty("severity") Severity severity, @JsonProperty("reaction") String reaction) {
    this.severity = severity;
    this.reaction = reaction;
  }

  public Severity getSeverity() {
    return severity;
  }

  public String getReaction() {
    return reaction;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof MonitoringDetails)) {
      return false;
    }
    MonitoringDetails that = (MonitoringDetails) o;
    return severity == that.severity && Objects.equals(reaction, that.reaction);
  }

  @Override
  public int hashCode() {
    return Objects.hash(severity, reaction);
  }

  @Override
  public String toString() {
    return "MonitoringDetails{" + "severity=" + severity + ", reaction='" + reaction + '\'' + '}';
  }

  public enum Severity {
    @JsonProperty("CRITICAL")
    CRITICAL,
    @JsonProperty("IMPORTANT")
    IMPORTANT,
    @JsonProperty("NON_IMPORTANT")
    NON_IMPORTANT
  }
}
