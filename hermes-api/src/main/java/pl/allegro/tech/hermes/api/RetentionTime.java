package pl.allegro.tech.hermes.api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class RetentionTime {
  private static final TimeUnit DEFAULT_UNIT = TimeUnit.DAYS;

  public static RetentionTime MAX = new RetentionTime(7, TimeUnit.DAYS);
  public static Set<TimeUnit> allowedUnits =
      EnumSet.of(TimeUnit.SECONDS, TimeUnit.MINUTES, TimeUnit.HOURS, TimeUnit.DAYS);

  @Min(0)
  private final int duration;

  private final TimeUnit retentionUnit;

  public RetentionTime(
      @JsonProperty("duration") int duration, @JsonProperty("retentionUnit") TimeUnit unit) {
    this.duration = duration;
    this.retentionUnit = unit == null ? DEFAULT_UNIT : unit;
  }

  public static RetentionTime of(int duration, TimeUnit unit) {
    return new RetentionTime(duration, unit);
  }

  public int getDuration() {
    return duration;
  }

  @JsonIgnore
  public long getDurationInMillis() {
    return retentionUnit.toMillis(duration);
  }

  public TimeUnit getRetentionUnit() {
    return retentionUnit;
  }

  @Override
  public int hashCode() {
    return Objects.hash(duration);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final RetentionTime other = (RetentionTime) obj;
    return Objects.equals(this.duration, other.duration)
        && Objects.equals(this.retentionUnit, other.retentionUnit);
  }
}
