package pl.allegro.tech.hermes.management.api;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Controller;
import pl.allegro.tech.hermes.tracker.management.TrackingUrlProvider;

@Controller
public class TrackingUrlsEndpoint {
  private final Optional<TrackingUrlProvider> trackingUrlProvider;

  public TrackingUrlsEndpoint(Optional<TrackingUrlProvider> trackingUrlProvider) {
    this.trackingUrlProvider = trackingUrlProvider;
  }

  @GET
  @Path("/topics/{topic}/tracking-urls")
  @Produces(APPLICATION_JSON)
  public Response getTopicTrackingUrls(@PathParam("topic") String topic) {
    return trackingUrlProvider
        .map(provider -> Response.ok(provider.getTrackingUrlsForTopic(topic)))
        .orElse(Response.ok(List.of()))
        .build();
  }

  @GET
  @Path("/topics/{topic}/subscriptions/{subscription}/tracking-urls")
  @Produces(APPLICATION_JSON)
  public Response getSubscriptionTrackingUrls(
      @PathParam("topic") String topic, @PathParam("subscription") String subscription) {
    return trackingUrlProvider
        .map(provider -> Response.ok(provider.getTrackingUrlsForSubscription(topic, subscription)))
        .orElse(Response.ok(List.of()))
        .build();
  }
}
