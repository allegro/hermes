package pl.allegro.tech.hermes.consumers;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import java.util.List;
import pl.allegro.tech.hermes.consumers.supervisor.process.RunningSubscriptionStatus;

@Path("status")
public interface ConsumerEndpoint {

  @GET
  @Produces(APPLICATION_JSON)
  @Path("/subscriptions")
  List<RunningSubscriptionStatus> listSubscriptions();

  @GET
  @Path("/subscriptionsCount")
  Integer countSubscriptions();

  @GET
  @Path("/health")
  String health();
}
