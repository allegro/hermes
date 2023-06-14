package pl.allegro.tech.hermes.api.endpoints;

import pl.allegro.tech.hermes.api.Subscription;

import java.util.List;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("subscriptions/owner")
public interface SubscriptionOwnershipEndpoint {
    @GET
    @Produces(APPLICATION_JSON)
    @Path("/{ownerSourceName}/{ownerId}")
    List<Subscription> listForOwner(@PathParam("ownerSourceName") String ownerSourceName,
                                    @PathParam("ownerId") String ownerId);
}
