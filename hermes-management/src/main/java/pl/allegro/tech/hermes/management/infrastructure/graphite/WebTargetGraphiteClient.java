package pl.allegro.tech.hermes.management.infrastructure.graphite;

import com.google.common.base.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static org.apache.commons.lang.exception.ExceptionUtils.getRootCauseMessage;

public class WebTargetGraphiteClient implements GraphiteClient {

    private static final Logger logger = LoggerFactory.getLogger(WebTargetGraphiteClient.class);

    private static final String DEFAULT_VALUE = "0.0";

    private static final String TARGET_PARAM = "target";

    private final WebTarget webTarget;

    public WebTargetGraphiteClient(WebTarget webTarget) {
        this.webTarget = webTarget
                .path("render")
                .queryParam("from", "-5minutes")
                .queryParam("until", "-1minutes")
                .queryParam("format", "json");
    }

    @Override
    public GraphiteMetrics readMetrics(String... metricPaths) {
        try {
            GraphiteMetrics response = new GraphiteMetrics();
            queryGraphite(metricPaths).stream().forEach(metric -> response.addMetricValue(metric.getTarget(), getFirstValue(metric)));
            return response;
        } catch (Exception exception) {
            logger.warn("Unable to read from Graphite. {}", getRootCauseMessage(exception));
            return GraphiteMetrics.unavailable(metricPaths);
        }
    }

    private String getFirstValue(GraphiteResponse graphiteResponse) {
        checkArgument(hasDatapoints(graphiteResponse), "Graphite format changed. Reexamine implementation.");

        String firstNotNullValue = DEFAULT_VALUE;
        for (List<String> datapoint : graphiteResponse.getDatapoints()) {
            if (datapointValid(datapoint)) {
                firstNotNullValue = datapoint.get(0);
                break;
            }
        }
        return firstNotNullValue;
    }

    private boolean datapointValid(List<String> value) {
        return !value.isEmpty() && !Strings.isNullOrEmpty(value.get(0)) && !"null".equals(value.get(0));
    }

    private boolean hasDatapoints(GraphiteResponse graphiteResponse) {
        return !graphiteResponse.getDatapoints().isEmpty() && !graphiteResponse.getDatapoints().get(0).isEmpty();
    }

    private List<GraphiteResponse> queryGraphite(String... queries) {
        WebTarget webQuery = webTarget;
        for (String query : queries) {
            webQuery = webQuery.queryParam(TARGET_PARAM, query);
        }
        return webQuery.request(MediaType.APPLICATION_JSON).get().readEntity(new GraphiteResponseList());
    }

    private static class GraphiteResponseList extends GenericType<List<GraphiteResponse>> {
    }
}
