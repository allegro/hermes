package pl.allegro.tech.hermes.management.infrastructure.prometheus;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import pl.allegro.tech.hermes.api.MetricDecimalValue;

import java.util.List;
import java.util.Optional;

record PrometheusResponse(@JsonProperty("status") String status,
                          @JsonProperty("data") Data data) {

    boolean isSuccess() {
        return status.equals("success") && data.isVector();
    }

    MetricDecimalValue getValue() {
        return data.getValue();
    }

    record Data(@JsonProperty("resultType") String resultType,
                @JsonProperty("result") List<Result> results) {
        boolean isVector() {
            return resultType.equals("vector");
        }

        MetricDecimalValue getValue() {
            return results.stream()
                    .findFirst()
                    .flatMap(Result::getValue)
                    .map(MetricDecimalValue::of)
                    .orElse(MetricDecimalValue.unavailable());
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Result(@JsonProperty("value") List<String> values) {
        Optional<String> getValue() {
            if (values.isEmpty() || values.stream().findFirst().get().length() != 2) {
                return Optional.empty();
            }
            return Optional.of(values.get(1));
        }
    }

}
