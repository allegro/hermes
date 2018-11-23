package pl.allegro.tech.hermes.management.api;

import java.util.List;
import java.util.Optional;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import org.springframework.beans.factory.annotation.Autowired;
import pl.allegro.tech.hermes.api.OwnerId;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.UnhealthySubscription;
import pl.allegro.tech.hermes.management.domain.owner.OwnerSource;
import pl.allegro.tech.hermes.management.domain.owner.OwnerSourceNotFound;
import pl.allegro.tech.hermes.management.domain.owner.OwnerSources;
import pl.allegro.tech.hermes.management.domain.subscription.SubscriptionService;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("subscriptions/owner")
public class SubscriptionsOwnershipEndpoint {

    private final OwnerSources ownerSources;
    private final SubscriptionService subscriptionService;

    @Autowired
    public SubscriptionsOwnershipEndpoint(OwnerSources ownerSources,
                                          SubscriptionService subscriptionService) {
        this.ownerSources = ownerSources;
        this.subscriptionService = subscriptionService;
    }

    @GET
    @Produces(APPLICATION_JSON)
    @Path("/{ownerSourceName}/{ownerId}")
    public List<Subscription> listForOwner(@PathParam("ownerSourceName") String ownerSourceName, @PathParam("ownerId") String id) {
        OwnerId ownerId = resolveOwnerId(ownerSourceName, id);
        return subscriptionService.getForOwnerId(ownerId);
    }

    @GET
    @Produces(APPLICATION_JSON)
    @Path("/unhealthy")
    public List<UnhealthySubscription> listUnhealthy(@QueryParam("ownerSourceName") String ownerSourceName,
                                                     @QueryParam("ownerId") String id,
                                                     @QueryParam("respectMonitoringSeverity") boolean respectMonitoringSeverity) {
        try {
            OwnerId ownerId = resolveOwnerId(ownerSourceName, id);
            return subscriptionService.getUnhealthyForOwner(ownerId);
        } catch (OwnerSource.OwnerNotFound | OwnerSourceNotFound e) {
            return subscriptionService.getAllUnhealthy();
        }
    }

    private OwnerId resolveOwnerId(String ownerSourceName, String id) {
        OwnerSource ownerSource = ownerSources.getByName(ownerSourceName)
                .orElseThrow(() -> new OwnerSourceNotFound(ownerSourceName));
        if (!ownerSource.exists(id)) {
            throw new OwnerSource.OwnerNotFound(ownerSourceName, id);
        }
        return new OwnerId(ownerSource.name(), id);
    }
}
