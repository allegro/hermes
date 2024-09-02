package pl.allegro.tech.hermes.management.api;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.GenericEntity;
import jakarta.ws.rs.core.Response;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;
import pl.allegro.tech.hermes.api.InconsistentGroup;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.management.api.auth.HermesSecurityAwareRequestUser;
import pl.allegro.tech.hermes.management.api.auth.Roles;
import pl.allegro.tech.hermes.management.domain.consistency.DcConsistencyService;
import pl.allegro.tech.hermes.management.domain.consistency.KafkaHermesConsistencyService;

@Component
@RolesAllowed(Roles.ADMIN)
@Path("consistency")
public class ConsistencyEndpoint {
  private final DcConsistencyService dcConsistencyService;
  private final KafkaHermesConsistencyService kafkaHermesConsistencyService;

  public ConsistencyEndpoint(
      DcConsistencyService dcConsistencyService,
      KafkaHermesConsistencyService kafkaHermesConsistencyService) {
    this.dcConsistencyService = dcConsistencyService;
    this.kafkaHermesConsistencyService = kafkaHermesConsistencyService;
  }

  @GET
  @Produces({APPLICATION_JSON})
  @Path("/inconsistencies/groups")
  public Response listInconsistentGroups(@QueryParam("groupNames") List<String> groupNames) {
    List<InconsistentGroup> inconsistentGroups =
        dcConsistencyService.listInconsistentGroups(new HashSet<>(groupNames));
    return Response.ok()
        .entity(new GenericEntity<List<InconsistentGroup>>(inconsistentGroups) {})
        .build();
  }

  @POST
  @Produces({APPLICATION_JSON})
  @Path("/sync/groups/{groupName}")
  public Response syncGroup(
      @PathParam("groupName") String groupName,
      @QueryParam("primaryDatacenter") String primaryDatacenter) {
    dcConsistencyService.syncGroup(groupName, primaryDatacenter);
    return Response.ok().build();
  }

  @POST
  @Produces({APPLICATION_JSON})
  @Path("/sync/topics/{topicName}")
  public Response syncTopic(
      @PathParam("topicName") String topicName,
      @QueryParam("primaryDatacenter") String primaryDatacenter) {
    dcConsistencyService.syncTopic(TopicName.fromQualifiedName(topicName), primaryDatacenter);
    return Response.ok().build();
  }

  @POST
  @Produces({APPLICATION_JSON})
  @Path("/sync/topics/{topicName}/subscriptions/{subscriptionName}")
  public Response syncSubscription(
      @PathParam("topicName") String topicName,
      @PathParam("subscriptionName") String subscriptionName,
      @QueryParam("primaryDatacenter") String primaryDatacenter) {
    SubscriptionName name =
        new SubscriptionName(subscriptionName, TopicName.fromQualifiedName(topicName));
    dcConsistencyService.syncSubscription(name, primaryDatacenter);
    return Response.ok().build();
  }

  @GET
  @Produces({APPLICATION_JSON})
  @Path("/inconsistencies/topics")
  public Response listInconsistentTopics() {
    return Response.ok(
            new GenericEntity<Set<String>>(
                kafkaHermesConsistencyService.listInconsistentTopics()) {})
        .build();
  }

  @DELETE
  @Produces({APPLICATION_JSON})
  @Path("/inconsistencies/topics")
  public Response removeTopicByName(
      @QueryParam("topicName") String topicName, @Context ContainerRequestContext requestContext) {
    kafkaHermesConsistencyService.removeTopic(
        topicName, new HermesSecurityAwareRequestUser(requestContext));
    return Response.ok().build();
  }

  @GET
  @Produces({APPLICATION_JSON})
  @Path("/groups")
  public Response listAllGroups() {
    Set<String> groupNames = dcConsistencyService.listAllGroupNames();
    return Response.ok().entity(new GenericEntity<Set<String>>(groupNames) {}).build();
  }
}
