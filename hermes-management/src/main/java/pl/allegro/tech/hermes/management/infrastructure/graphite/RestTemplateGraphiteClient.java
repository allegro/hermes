package pl.allegro.tech.hermes.management.infrastructure.graphite;

import com.google.common.base.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import pl.allegro.tech.hermes.api.MetricDecimalValue;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.List;
import javax.ws.rs.core.UriBuilder;

import static com.google.common.base.Preconditions.checkArgument;
import static org.apache.commons.lang.exception.ExceptionUtils.getRootCauseMessage;

public class RestTemplateGraphiteClient implements GraphiteClient {

    private static final Logger logger = LoggerFactory.getLogger(RestTemplateGraphiteClient.class);

    private static final ParameterizedTypeReference<List<GraphiteResponse>> GRAPHITE_RESPONSE_TYPE =
            new ParameterizedTypeReference<>() {
            };

    private static final String DEFAULT_VALUE = "0.0";

    private static final String TARGET_PARAM = "target";

    private final URI graphiteUri;

    private final RestTemplate restTemplate;

    public RestTemplateGraphiteClient(RestTemplate restTemplate, URI graphiteUri) {
        this.restTemplate = restTemplate;
        this.graphiteUri = graphiteUri;
    }

    @Override
    public GraphiteMetrics readMetrics(String... metricPaths) {
        try {
            GraphiteMetrics response = new GraphiteMetrics();
            queryGraphite(metricPaths).stream().forEach(metric -> response.addMetricValue(metric.getTarget(), getFirstValue(metric)));
            return response;
        } catch (Exception exception) {
            logger.warn("Unable to read from Graphite: {}", getRootCauseMessage(exception));
            return GraphiteMetrics.unavailable(metricPaths);
        }
    }

    private MetricDecimalValue getFirstValue(GraphiteResponse graphiteResponse) {
        checkArgument(hasDatapoints(graphiteResponse), "Graphite format changed. Reexamine implementation.");

        String firstNotNullValue = DEFAULT_VALUE;
        for (List<String> datapoint : graphiteResponse.getDatapoints()) {
            if (datapointValid(datapoint)) {
                firstNotNullValue = datapoint.get(0);
                break;
            }
        }
        return MetricDecimalValue.of(firstNotNullValue);
    }

    private boolean datapointValid(List<String> value) {
        return !value.isEmpty() && !Strings.isNullOrEmpty(value.get(0)) && !"null".equals(value.get(0));
    }

    private boolean hasDatapoints(GraphiteResponse graphiteResponse) {
        return !graphiteResponse.getDatapoints().isEmpty() && !graphiteResponse.getDatapoints().get(0).isEmpty();
    }

    private List<GraphiteResponse> queryGraphite(String... queries) throws UnsupportedEncodingException {
        UriBuilder builder = UriBuilder.fromUri(graphiteUri)
                .path("render")
                .queryParam("from", "-5minutes")
                .queryParam("until", "-1minutes")
                .queryParam("format", "json");

        for (String query : queries) {
            builder.queryParam(TARGET_PARAM, query);
        }

        ResponseEntity<List<GraphiteResponse>> response = restTemplate.exchange(
                builder.build(),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                GRAPHITE_RESPONSE_TYPE
        );
        return response.getBody();
    }
}
