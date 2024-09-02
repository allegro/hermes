package pl.allegro.tech.hermes.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import java.util.Objects;

public class OfflineRetentionTime {

  @Min(1)
  private final Integer duration;

  private final boolean infinite;

  public OfflineRetentionTime(
      @JsonProperty("duration") Integer duration, @JsonProperty("infinite") boolean infinite) {
    this.infinite = infinite;
    this.duration = infinite ? null : duration;
  }

  public static OfflineRetentionTime of(int duration) {
    return new OfflineRetentionTime(duration, false);
  }

  public static OfflineRetentionTime infinite() {
    return new OfflineRetentionTime(null, false);
  }

  public Integer getDuration() {
    return duration;
  }

  public boolean isInfinite() {
    return infinite;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof OfflineRetentionTime)) {
      return false;
    }
    OfflineRetentionTime that = (OfflineRetentionTime) o;
    return infinite == that.infinite && Objects.equals(duration, that.duration);
  }

  @Override
  public int hashCode() {
    return Objects.hash(duration, infinite);
  }
}
