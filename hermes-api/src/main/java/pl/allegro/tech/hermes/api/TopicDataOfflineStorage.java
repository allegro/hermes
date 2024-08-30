package pl.allegro.tech.hermes.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.Objects;

/**
 * Topic offline storage metadata - not used in Hermes, but exposed as part of API for other systems
 * to use.
 */
public class TopicDataOfflineStorage {

  private final boolean enabled;

  @Valid @NotNull private final OfflineRetentionTime retentionTime;

  @JsonCreator
  public TopicDataOfflineStorage(
      @JsonProperty("enabled") boolean enabled,
      @JsonProperty("retentionTime") OfflineRetentionTime retentionTime) {
    this.enabled = enabled;
    this.retentionTime = retentionTime;
  }

  public static TopicDataOfflineStorage defaultOfflineStorage() {
    return new TopicDataOfflineStorage(false, OfflineRetentionTime.of(0));
  }

  public boolean isEnabled() {
    return enabled;
  }

  public OfflineRetentionTime getRetentionTime() {
    return retentionTime;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof TopicDataOfflineStorage)) {
      return false;
    }
    TopicDataOfflineStorage that = (TopicDataOfflineStorage) o;
    return enabled == that.enabled && Objects.equals(retentionTime, that.retentionTime);
  }

  @Override
  public int hashCode() {
    return Objects.hash(enabled, retentionTime);
  }
}
