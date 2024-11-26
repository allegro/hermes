package pl.allegro.tech.hermes.management.api;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static pl.allegro.tech.hermes.management.infrastructure.audit.AuditEventType.REMOVED;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import org.springframework.beans.factory.annotation.Autowired;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.management.domain.subscription.SubscriptionService;
import pl.allegro.tech.hermes.management.infrastructure.audit.AuditEvent;

@Path("subscriptions/termination-cleanup")
public class SubscriptionTerminationCleanupEndpoint {

  private final SubscriptionService subscriptionService;

  @Autowired
  public SubscriptionTerminationCleanupEndpoint(SubscriptionService subscriptionService) {
    this.subscriptionService = subscriptionService;
  }

  @POST
  @Consumes(APPLICATION_JSON)
  @Path("/from-audit-event")
  public Response subscriptionTerminationCleanup(AuditEvent auditEvent) {
    if (auditEvent.getEventType() != REMOVED
        || !auditEvent.getPayloadClass().equals(Subscription.class.getName())) {
      return Response.ok().build();
    }

    SubscriptionName subscriptionName =
        Subscription.getSubscriptionNameFromString(auditEvent.getResourceName());
    subscriptionService.subscriptionTerminationCleanup(subscriptionName);
    return Response.ok().build();
  }
}
