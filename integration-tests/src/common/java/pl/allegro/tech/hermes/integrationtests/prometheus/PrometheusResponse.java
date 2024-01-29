package pl.allegro.tech.hermes.integrationtests.prometheus;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

record PrometheusResponse(@JsonProperty("status") String status,
                          @JsonProperty("data") Data data) {

    record Data(@JsonProperty("resultType") String resultType,
                @JsonProperty("result") List<Result> results) {
    }

    record Result(@JsonProperty("metric") MetricName metricName,
                  @JsonProperty("value") List<String> values) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record MetricName(@JsonProperty(value = "__name__") String name,
                      @JsonProperty(value = "status_code") String statusCode) {
    }
}
