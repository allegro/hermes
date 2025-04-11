package pl.allegro.tech.hermes.management.infrastructure.prometheus;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

record PrometheusResponse(@JsonProperty("status") String status, @JsonProperty("data") Data data) {

  boolean isSuccess() {
    return status.equals("success") && data.isVector();
  }

  record Data(
      @JsonProperty("resultType") String resultType,
      @JsonProperty("result") List<VectorResult> results) {
    boolean isVector() {
      return resultType.equals("vector");
    }

    public boolean isHistogram() {
      return !results.isEmpty()
          && results.stream()
              .allMatch(
                  vectorResult ->
                      vectorResult.metric() != null && vectorResult.metric().le() != null);
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  record VectorResult(
      @JsonProperty("metric") Metric metric, @JsonProperty("value") List<String> vector) {

    private static final int VALID_VECTOR_LENGTH = 2;
    private static final int SCALAR_INDEX_VALUE = 1;

    Optional<Double> getDoubleValue() {
      return getValue(Double::parseDouble);
    }

    Optional<Long> getLongValue() {
      return getValue(Long::parseLong);
    }

    private <T> Optional<T> getValue(Function<String, T> parser) {
      if (vector.size() != VALID_VECTOR_LENGTH) {
        return Optional.empty();
      }
      return Optional.of(parser.apply(vector.get(SCALAR_INDEX_VALUE)));
    }

    record Metric(@JsonProperty("le") String le) {}
  }
}
