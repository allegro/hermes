package pl.allegro.tech.hermes.integrationtests.prometheus;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

record PrometheusResponse(@JsonProperty("status") String status, @JsonProperty("data") Data data) {

  record Data(
      @JsonProperty("resultType") String resultType,
      @JsonProperty("result") List<Result> results) {}

  record Result(@JsonProperty("value") List<String> values) {}
}
