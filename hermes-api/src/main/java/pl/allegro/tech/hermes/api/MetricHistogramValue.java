package pl.allegro.tech.hermes.api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Map;

public record MetricHistogramValue(@JsonIgnore boolean available, Map<String, String> buckets)
    implements MetricValue {

  private static final MetricHistogramValue UNAVAILABLE =
      new MetricHistogramValue(false, Map.of("+Inf", "-1"));
  private static final String DEFAULT_BUCKET_VALUE = "0";
  private static final MetricHistogramValue DEFAULT_VALUE =
      new MetricHistogramValue(true, Map.of("+Inf", DEFAULT_BUCKET_VALUE));

  public static MetricHistogramValue ofBuckets(Map<String, String> buckets) {
    return new MetricHistogramValue(true, buckets);
  }

  public static MetricHistogramValue ofBuckets(String le1, String v1) {
    return new MetricHistogramValue(true, Map.of(le1, v1));
  }

  public static MetricHistogramValue ofBuckets(String le1, String v1, String le2, String v2) {
    return new MetricHistogramValue(true, Map.of(le1, v1, le2, v2));
  }

  public static MetricHistogramValue ofBuckets(
      String le1, String v1, String le2, String v2, String le3, String v3) {
    return new MetricHistogramValue(true, Map.of(le1, v1, le2, v2, le3, v3));
  }

  public static MetricHistogramValue unavailable() {
    return UNAVAILABLE;
  }

  public static MetricHistogramValue defaultValue() {
    return DEFAULT_VALUE;
  }

  public static String defaultBucketValue() {
    return DEFAULT_BUCKET_VALUE;
  }

  @Override
  public boolean isAvailable() {
    return available;
  }
}
