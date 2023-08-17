package pl.allegro.tech.hermes.management.infrastructure.prometheus;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Optional;

record PrometheusResponse(@JsonProperty("status") String status,
                          @JsonProperty("data") Data data) {

    boolean isSuccess() {
        return status.equals("success") && data.isVector();
    }

    record Data(@JsonProperty("resultType") String resultType,
                @JsonProperty("result") List<Result> results) {
        boolean isVector() {
            return resultType.equals("vector");
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Result(
            @JsonProperty("metric") MetricName metricName,
            @JsonProperty("value") List<String> values) {
        Optional<Double> getValue() {
            if (values.size() != 2) {
                return Optional.empty();
            }
            return Optional.of(Double.parseDouble(values.get(1)));
        }

        Result renameMetric(String newMetricName) {
            return new Result(new MetricName(newMetricName, metricName.statusCode), values);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record MetricName(
            @JsonProperty(value = "__name__") String name,
            @JsonProperty(value = "status_code") Optional<String> statusCode) {
        boolean is2xxStatusCode() {
            return hasStatusCode() && statusCode.get().startsWith("2");
        }

        boolean is4xxStatusCode() {
            return hasStatusCode() && statusCode.get().startsWith("4");
        }

        boolean is5xxStatusCode() {
            return hasStatusCode() && statusCode.get().startsWith("5");
        }

        private boolean hasStatusCode() {
            return statusCode.isPresent();
        }
    }

}
