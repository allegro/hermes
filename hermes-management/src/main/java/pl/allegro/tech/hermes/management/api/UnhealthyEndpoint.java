package pl.allegro.tech.hermes.management.api;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import org.springframework.beans.factory.annotation.Autowired;
import pl.allegro.tech.hermes.api.OwnerId;
import pl.allegro.tech.hermes.api.UnhealthySubscription;
import pl.allegro.tech.hermes.management.domain.owner.OwnerSource;
import pl.allegro.tech.hermes.management.domain.owner.OwnerSourceNotFound;
import pl.allegro.tech.hermes.management.domain.owner.OwnerSources;
import pl.allegro.tech.hermes.management.domain.subscription.SubscriptionService;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

@Path("unhealthy")
public class UnhealthyEndpoint {

    private final OwnerSources ownerSources;
    private final SubscriptionService subscriptionService;

    @Autowired
    public UnhealthyEndpoint(OwnerSources ownerSources,
                             SubscriptionService subscriptionService) {
        this.ownerSources = ownerSources;
        this.subscriptionService = subscriptionService;
    }

    @GET
    @Produces(APPLICATION_JSON)
    @Path("/")
    public List<UnhealthySubscription> listUnhealthy(
            @QueryParam("ownerSourceName") String ownerSourceName,
            @QueryParam("ownerId") String id,
            @DefaultValue("true") @QueryParam("respectMonitoringSeverity") boolean respectMonitoringSeverity) {

        Optional<OwnerId> ownerId = resolveOwnerId(ownerSourceName, id);
        return ownerId.isPresent()
                ? subscriptionService.getUnhealthyForOwner(ownerId.get(), respectMonitoringSeverity)
                : subscriptionService.getAllUnhealthy(respectMonitoringSeverity);
    }

    private Optional<OwnerId> resolveOwnerId(String ownerSourceName, String id) {
        return ownerSources.getByName(ownerSourceName).map(ownerSource -> new OwnerId(ownerSource.name(), id));
    }
}
