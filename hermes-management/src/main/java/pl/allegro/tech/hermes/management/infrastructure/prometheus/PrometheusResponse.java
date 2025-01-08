package pl.allegro.tech.hermes.management.infrastructure.prometheus;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Optional;

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
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  record VectorResult(@JsonProperty("value") List<String> vector) {

    private static final int VALID_VECTOR_LENGTH = 2;
    private static final int SCALAR_INDEX_VALUE = 1;

    Optional<Double> getValue() {
      if (vector.size() != VALID_VECTOR_LENGTH) {
        return Optional.empty();
      }
      return Optional.of(Double.parseDouble(vector.get(SCALAR_INDEX_VALUE)));
    }
  }
}
