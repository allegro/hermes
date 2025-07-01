package pl.allegro.tech.hermes.api.subscription.metrics;

import static java.util.Collections.emptyList;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;

public record MessageProcessingDurationMetricOptions(
    @Size(max = 10) List<@Positive Long> thresholdsMilliseconds, boolean enabled) {

  public static MessageProcessingDurationMetricOptions DISABLED =
      MessageProcessingDurationMetricOptions.disabled();

  public static MessageProcessingDurationMetricOptions of(Long... thresholdsMilliseconds) {
    return new MessageProcessingDurationMetricOptions(Arrays.asList(thresholdsMilliseconds), true);
  }

  public static MessageProcessingDurationMetricOptions disabled() {
    return new MessageProcessingDurationMetricOptions(emptyList(), false);
  }

  @JsonIgnore
  public Duration[] getThresholdsDurations() {
    return thresholdsMilliseconds.stream().map(Duration::ofMillis).toArray(Duration[]::new);
  }

  public boolean hasThresholds() {
    return !thresholdsMilliseconds.isEmpty();
  }
}
