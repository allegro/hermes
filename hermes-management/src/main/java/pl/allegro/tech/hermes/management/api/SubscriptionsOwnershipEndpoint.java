package pl.allegro.tech.hermes.management.api;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import pl.allegro.tech.hermes.api.OwnerId;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.management.domain.owner.OwnerSource;
import pl.allegro.tech.hermes.management.domain.owner.OwnerSourceNotFound;
import pl.allegro.tech.hermes.management.domain.owner.OwnerSources;
import pl.allegro.tech.hermes.management.domain.subscription.SubscriptionService;

@Path("subscriptions/owner")
public class SubscriptionsOwnershipEndpoint {

  private final OwnerSources ownerSources;
  private final SubscriptionService subscriptionService;

  @Autowired
  public SubscriptionsOwnershipEndpoint(
      OwnerSources ownerSources, SubscriptionService subscriptionService) {
    this.ownerSources = ownerSources;
    this.subscriptionService = subscriptionService;
  }

  @GET
  @Produces(APPLICATION_JSON)
  @Path("/{ownerSourceName}/{ownerId}")
  public List<Subscription> listForOwner(
      @PathParam("ownerSourceName") String ownerSourceName, @PathParam("ownerId") String id) {
    OwnerId ownerId = resolveOwnerId(ownerSourceName, id);
    return subscriptionService.getForOwnerId(ownerId);
  }

  private OwnerId resolveOwnerId(String ownerSourceName, String id) {
    OwnerSource ownerSource =
        ownerSources
            .getByName(ownerSourceName)
            .orElseThrow(() -> new OwnerSourceNotFound(ownerSourceName));
    if (!ownerSource.exists(id)) {
      throw new OwnerSource.OwnerNotFound(ownerSourceName, id);
    }
    return new OwnerId(ownerSource.name(), id);
  }
}
