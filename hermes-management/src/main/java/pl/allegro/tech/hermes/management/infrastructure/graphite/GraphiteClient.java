package pl.allegro.tech.hermes.management.infrastructure.graphite;

import com.google.common.base.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;

public class GraphiteClient {

    private static final Logger logger = LoggerFactory.getLogger(GraphiteClient.class);

    private static final String DEFAULT_VALUE = "0.0";

    private static final String TARGET_PARAM = "target";

    private final WebTarget webTarget;

    public GraphiteClient(WebTarget webTarget) {
        this.webTarget = webTarget
                .path("render")
                .queryParam("from", "-1minutes")
                .queryParam("until", "now")
                .queryParam("format", "json");
    }

    public GraphiteMetrics readMetrics(String... metrics) {
        try {
            GraphiteMetrics response = new GraphiteMetrics();
            queryGraphite(metrics).stream().forEach(metric -> response.addMetricValue(metric.getTarget(), getFirstValue(metric)));
            return response;
        } catch (GraphiteConnectionException exception) {
            logger.warn("Unable to read from Graphite", exception.getMessage());
            return GraphiteMetrics.unavailable(metrics);
        }
    }

    private String getFirstValue(GraphiteResponse graphiteResponse) {
        checkArgument(hasDatapoints(graphiteResponse), "Graphite format changed. Reexamine implementation.");
        String value = graphiteResponse.getDatapoints().get(0).get(0);

        return Strings.isNullOrEmpty(value) || "null".equals(value) ? DEFAULT_VALUE : value;
    }

    private boolean hasDatapoints(GraphiteResponse graphiteResponse) {
        return !graphiteResponse.getDatapoints().isEmpty() && !graphiteResponse.getDatapoints().get(0).isEmpty();
    }

    private List<GraphiteResponse> queryGraphite(String... queries) {
        WebTarget webQuery = webTarget;
        for (String query : queries) {
            webQuery = webQuery.queryParam(TARGET_PARAM, query);
        }
        try {
            return webQuery.request(MediaType.APPLICATION_JSON).get().readEntity(new GraphiteResponseList());
        } catch (Exception exception) {
            throw new GraphiteConnectionException(exception);
        }
    }

    protected static class GraphiteResponseList extends GenericType<List<GraphiteResponse>> {
    }
}
