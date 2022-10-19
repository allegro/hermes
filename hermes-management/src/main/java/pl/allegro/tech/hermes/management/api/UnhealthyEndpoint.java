package pl.allegro.tech.hermes.management.api;

import org.springframework.beans.factory.annotation.Autowired;
import pl.allegro.tech.hermes.api.OwnerId;
import pl.allegro.tech.hermes.api.UnhealthySubscription;
import pl.allegro.tech.hermes.management.domain.owner.OwnerSource;
import pl.allegro.tech.hermes.management.domain.owner.OwnerSources;
import pl.allegro.tech.hermes.management.domain.subscription.SubscriptionService;

import java.util.List;
import java.util.Optional;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.Response;

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

        List<UnhealthySubscription> unhealthySubscriptions = areEmpty(ownerSourceName, id)
                ? subscriptionService.getAllUnhealthy(respectMonitoringSeverity, subscriptionNames, qualifiedTopicNames)
                : resolveOwnerId(ownerSourceName, id)
                .map(ownerId -> subscriptionService.getUnhealthyForOwner(
                        ownerId, respectMonitoringSeverity, subscriptionNames, qualifiedTopicNames
                ))
                .orElseThrow(() -> new OwnerSource.OwnerNotFound(ownerSourceName, id));
        return Response.ok()
                .entity(new GenericEntity<List<UnhealthySubscription>>(unhealthySubscriptions) {
                })
                .build();
    }

    private boolean areEmpty(String ownerSourceName, String id) {
        return ownerSourceName == null && id == null;
    }

    private Optional<OwnerId> resolveOwnerId(String ownerSourceName, String id) {
        return ownerSources.getByName(ownerSourceName).map(ownerSource -> new OwnerId(ownerSource.name(), id));
    }
}
