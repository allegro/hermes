package pl.allegro.tech.hermes.management.infrastructure.graphite;

import java.util.List;

public class GraphiteResponse {

    private String target;
    private List<List<String>> datapoints;

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public List<List<String>> getDatapoints() {
        return datapoints;
    }

    public void setDatapoints(List<List<String>> datapoints) {
        this.datapoints = datapoints;
    }
}
