package pl.allegro.tech.hermes.management.api;

import org.springframework.beans.factory.annotation.Autowired;
import pl.allegro.tech.hermes.api.OwnerId;
import pl.allegro.tech.hermes.api.UnhealthySubscription;
import pl.allegro.tech.hermes.management.domain.owner.OwnerSources;
import pl.allegro.tech.hermes.management.domain.subscription.SubscriptionService;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Optional;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;

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
    @Produces({APPLICATION_JSON, TEXT_PLAIN})
    @Path("/")
    public Response listUnhealthy(
            @QueryParam("ownerSourceName") String ownerSourceName,
            @QueryParam("ownerId") String id,
            @DefaultValue("true") @QueryParam("respectMonitoringSeverity") boolean respectMonitoringSeverity,
            @QueryParam("subscriptionNames") List<String> subscriptionNames,
            @QueryParam("qualifiedTopicNames") List<String> qualifiedTopicNames) {

        Optional<OwnerId> ownerId = resolveOwnerId(ownerSourceName, id);
        List<UnhealthySubscription> unhealthySubscriptions = ownerId.isPresent()
                ? subscriptionService.getUnhealthyForOwner(ownerId.get(), respectMonitoringSeverity, subscriptionNames, qualifiedTopicNames)
                : subscriptionService.getAllUnhealthy(respectMonitoringSeverity, subscriptionNames, qualifiedTopicNames);
        return Response.ok()
                .entity(new GenericEntity<List<UnhealthySubscription>>(unhealthySubscriptions){})
                .build();
    }

    private Optional<OwnerId> resolveOwnerId(String ownerSourceName, String id) {
        return ownerSources.getByName(ownerSourceName).map(ownerSource -> new OwnerId(ownerSource.name(), id));
    }
}
