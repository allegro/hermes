package pl.allegro.tech.hermes.api.endpoints;

import java.util.List;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.UnhealthySubscription;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("subscriptions/owner")
public interface SubscriptionOwnershipEndpoint {
    @GET
    @Produces(APPLICATION_JSON)
    @Path("/{ownerSourceName}/{ownerId}")
    List<Subscription> listForOwner(@PathParam("ownerSourceName") String ownerSourceName,
                                    @PathParam("ownerId") String ownerId);

    @GET
    @Produces(APPLICATION_JSON)
    @Path("/{ownerSourceName}/{ownerId}/unhealthy")
    List<UnhealthySubscription> listUnhealthyForOwner(@PathParam("ownerSourceName") String ownerSourceName,
                                                      @PathParam("ownerId") String ownerId);
}
