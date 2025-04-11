package pl.allegro.tech.hermes.integrationtests.prometheus;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

record PrometheusResponse(@JsonProperty("status") String status, @JsonProperty("data") Data data) {

  record Data(
      @JsonProperty("resultType") String resultType,
      @JsonProperty("result") List<Result> results) {}

  record Result(@JsonProperty("metric") Metric metric, @JsonProperty("value") List<String> values) {
    public Result(List<String> values) {
      this(null, values);
    }
  }

  record Metric(@JsonProperty("le") String le) {}
}
