package pl.allegro.tech.hermes.management.api;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import pl.allegro.tech.hermes.api.ErrorCode;
import pl.allegro.tech.hermes.management.domain.ManagementException;
import pl.allegro.tech.hermes.management.domain.MetricsDashboardUrl;
import pl.allegro.tech.hermes.management.domain.MetricsDashboardUrlService;

@Component
@Path("/dashboards")
public class MetricsDashboardUrlEndpoint {

  private static final Logger logger = LoggerFactory.getLogger(MetricsDashboardUrlEndpoint.class);

  private final Optional<MetricsDashboardUrlService> metricsDashboardUrlService;

  MetricsDashboardUrlEndpoint(Optional<MetricsDashboardUrlService> metricsDashboardUrlService) {
    if (metricsDashboardUrlService.isEmpty()) {
      logger.info("Dashboards url bean is absent");
    }
    this.metricsDashboardUrlService = metricsDashboardUrlService;
  }

  @GET
  @Path("/topics/{topic}")
  @Produces(APPLICATION_JSON)
  public Response fetchUrlForTopic(@PathParam("topic") String topic) {
    return metricsDashboardUrlService
        .map(service -> Response.ok(new MetricsDashboardUrl(service.getUrlForTopic(topic))).build())
        .orElseThrow(MetricsDashboardUrlEndpoint.MetricsDashboardUrlAbsentException::new);
  }

  @GET
  @Path("/topics/{topic}/subscriptions/{subscription}")
  @Produces(APPLICATION_JSON)
  public Response fetchUrlForSubscription(
      @PathParam("topic") String topic, @PathParam("subscription") String subscription) {
    return metricsDashboardUrlService
        .map(
            service ->
                Response.ok(
                        new MetricsDashboardUrl(service.getUrlForSubscription(topic, subscription)))
                    .build())
        .orElseThrow(MetricsDashboardUrlEndpoint.MetricsDashboardUrlAbsentException::new);
  }

  private static class MetricsDashboardUrlAbsentException extends ManagementException {

    MetricsDashboardUrlAbsentException() {
      super("Dashboard url implementation is absent");
    }

    @Override
    public ErrorCode getCode() {
      return ErrorCode.IMPLEMENTATION_ABSENT;
    }
  }
}
