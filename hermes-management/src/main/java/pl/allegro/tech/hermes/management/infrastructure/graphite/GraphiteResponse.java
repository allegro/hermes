package pl.allegro.tech.hermes.management.infrastructure.graphite;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class GraphiteResponse {

    private final String target;
    private final List<List<String>> datapoints;

    @JsonCreator
    public GraphiteResponse(
            @JsonProperty("target") String target,
            @JsonProperty("datapoints") List<List<String>> datapoints
    ) {
        this.target = target;
        this.datapoints = datapoints;
    }

    public String getTarget() {
        return target;
    }

    public List<List<String>> getDatapoints() {
        return datapoints;
    }
}
