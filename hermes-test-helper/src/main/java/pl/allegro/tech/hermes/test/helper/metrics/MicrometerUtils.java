package pl.allegro.tech.hermes.test.helper.metrics;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.search.Search;
import java.util.Optional;
import java.util.function.Function;

public class MicrometerUtils {
  public static <T extends Meter, R> Optional<R> metricValue(
      MeterRegistry meterRegistry,
      String metricName,
      Tags tags,
      Function<Search, T> searchMapper,
      Function<T, R> metricValueMapper) {
    return Optional.ofNullable(searchMapper.apply(meterRegistry.find(metricName).tags(tags)))
        .map(metricValueMapper);
  }

  public static <T extends Meter, R> Optional<R> metricValue(
      MeterRegistry meterRegistry,
      String metricName,
      Function<Search, T> searchMapper,
      Function<T, R> metricValueMapper) {
    return metricValue(meterRegistry, metricName, Tags.empty(), searchMapper, metricValueMapper);
  }
}
