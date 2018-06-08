package pl.allegro.tech.hermes.consumers;

import pl.allegro.tech.hermes.consumers.supervisor.process.ManagedSubscriptionStatus;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import java.util.List;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("status")
public interface ConsumerEndpoint {

    @GET
    @Produces(APPLICATION_JSON)
    @Path("/subscriptions")
    List<ManagedSubscriptionStatus> listSubscriptions();

    @GET
    @Path("/subscriptionsCount")
    Integer countSubscriptions();

    @GET
    @Path("/health")
    String health();

}
