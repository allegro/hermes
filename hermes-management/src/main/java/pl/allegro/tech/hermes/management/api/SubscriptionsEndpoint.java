package pl.allegro.tech.hermes.management.api;

import com.wordnik.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import pl.allegro.tech.hermes.api.ConsumerGroup;
import pl.allegro.tech.hermes.api.MessageTrace;
import pl.allegro.tech.hermes.api.OffsetRetransmissionDate;
import pl.allegro.tech.hermes.api.PatchData;
import pl.allegro.tech.hermes.api.PersistentSubscriptionMetrics;
import pl.allegro.tech.hermes.api.Query;
import pl.allegro.tech.hermes.api.SentMessageTrace;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.SubscriptionHealth;
import pl.allegro.tech.hermes.api.SubscriptionMetrics;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.management.api.auth.ManagementRights;
import pl.allegro.tech.hermes.management.api.auth.Roles;
import pl.allegro.tech.hermes.management.domain.subscription.SubscriptionService;
import pl.allegro.tech.hermes.management.domain.topic.TopicService;
import pl.allegro.tech.hermes.management.infrastructure.kafka.MultiDCAwareService;
import pl.allegro.tech.hermes.management.infrastructure.kafka.MultiDCOffsetChangeSummary;

import javax.annotation.security.RolesAllowed;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.util.List;
import java.util.Optional;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.OK;
import static pl.allegro.tech.hermes.api.TopicName.fromQualifiedName;

@Path("topics/{topicName}/subscriptions")
public class SubscriptionsEndpoint {

    private final SubscriptionService subscriptionService;
    private final TopicService topicService;
    private final MultiDCAwareService multiDCAwareService;
    private final ManagementRights managementRights;

    @Autowired
    public SubscriptionsEndpoint(SubscriptionService subscriptionService,
                                 TopicService topicService,
                                 MultiDCAwareService multiDCAwareService,
                                 ManagementRights managementRights) {
        this.subscriptionService = subscriptionService;
        this.topicService = topicService;
        this.multiDCAwareService = multiDCAwareService;
        this.managementRights = managementRights;
    }

    @GET
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Lists subscriptions", response = List.class, httpMethod = HttpMethod.GET)
    public List<String> list(
            @PathParam("topicName") String qualifiedTopicName,
            @DefaultValue("false") @QueryParam("tracked") boolean tracked) {

        return tracked ?
                subscriptionService.listTrackedSubscriptionNames(fromQualifiedName(qualifiedTopicName)) :
                subscriptionService.listSubscriptionNames(fromQualifiedName(qualifiedTopicName));
    }

    @POST
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @Path("/query")
    @ApiOperation(value = "Queries subscriptions", response = List.class, httpMethod = HttpMethod.POST)
    public List<String> queryList(
            @PathParam("topicName") String qualifiedTopicName,
            Query<Subscription> query) {
        return subscriptionService.listFilteredSubscriptionNames(fromQualifiedName(qualifiedTopicName), query);
    }

    @POST
    @Consumes(APPLICATION_JSON)
    @RolesAllowed({Roles.ANY})
    @ApiOperation(value = "Create subscription", httpMethod = HttpMethod.POST)
    public Response create(@PathParam("topicName") String qualifiedTopicName,
                           Subscription subscription,
                           @Context ContainerRequestContext requestContext) {
        subscriptionService.createSubscription(subscription, requestContext.getSecurityContext().getUserPrincipal().getName(),
                managementRights.getSubscriptionCreatorRights(requestContext), qualifiedTopicName);
        return responseStatus(Response.Status.CREATED);
    }

    @GET
    @Produces(APPLICATION_JSON)
    @Path("/{subscriptionName}")
    @ApiOperation(value = "Get subscription details", response = Subscription.class, httpMethod = HttpMethod.GET)
    public Subscription get(@PathParam("topicName") String qualifiedTopicName,
                            @PathParam("subscriptionName") String subscriptionName) {
        return subscriptionService.getSubscriptionDetails(fromQualifiedName(qualifiedTopicName), subscriptionName);
    }

    @GET
    @Produces(APPLICATION_JSON)
    @Path("/{subscriptionName}/state")
    @ApiOperation(value = "Get subscription state", response = Subscription.State.class, httpMethod = HttpMethod.GET)
    public Subscription.State getState(@PathParam("topicName") String qualifiedTopicName,
                                       @PathParam("subscriptionName") String subscriptionName) {
        return subscriptionService.getSubscriptionState(fromQualifiedName(qualifiedTopicName), subscriptionName);
    }

    @GET
    @Produces(APPLICATION_JSON)
    @Path("/{subscriptionName}/undelivered/last")
    @ApiOperation(value = "Get latest undelivered message", response = SentMessageTrace.class, httpMethod = HttpMethod.GET)
    public Response getLatestUndeliveredMessage(@PathParam("topicName") String qualifiedTopicName,
                                                @PathParam("subscriptionName") String subscriptionName) {
        Optional<SentMessageTrace> result = subscriptionService.getLatestUndeliveredMessage(
                fromQualifiedName(qualifiedTopicName), subscriptionName
        );

        return result.isPresent() ? Response.status(OK).entity(result.get()).build() : responseStatus(NOT_FOUND);
    }

    @GET
    @Produces(APPLICATION_JSON)
    @Path("/{subscriptionName}/undelivered")
    @ApiOperation(value = "Get latest undelivered messages", response = List.class, httpMethod = HttpMethod.GET)
    public Response getLatestUndeliveredMessages(@PathParam("topicName") String qualifiedTopicName,
                                                 @PathParam("subscriptionName") String subscriptionName) {
        List<SentMessageTrace> result = subscriptionService.getLatestUndeliveredMessagesTrackerLogs(
                fromQualifiedName(qualifiedTopicName), subscriptionName
        );
        return Response.status(OK).entity(result).build();
    }

    @GET
    @Produces(APPLICATION_JSON)
    @Path("/{subscriptionName}/metrics")
    @ApiOperation(value = "Get subscription metrics", response = SubscriptionMetrics.class, httpMethod = HttpMethod.GET)
    public SubscriptionMetrics getMetrics(@PathParam("topicName") String qualifiedTopicName,
                                          @PathParam("subscriptionName") String subscriptionName) {
        return subscriptionService.getSubscriptionMetrics(fromQualifiedName(qualifiedTopicName), subscriptionName);
    }

    @GET
    @Produces(APPLICATION_JSON)
    @Path("/{subscriptionName}/metrics/persistent")
    @ApiOperation(value = "Get persistent subscription metrics", response = PersistentSubscriptionMetrics.class, httpMethod = HttpMethod.GET)
    public PersistentSubscriptionMetrics getPersistentMetrics(@PathParam("topicName") String qualifiedTopicName,
                                                              @PathParam("subscriptionName") String subscriptionName) {
        return subscriptionService.getPersistentSubscriptionMetrics(fromQualifiedName(qualifiedTopicName), subscriptionName);
    }

    @GET
    @Produces(APPLICATION_JSON)
    @Path("/{subscriptionName}/health")
    @ApiOperation(value = "Get subscription health", response = SubscriptionHealth.class, httpMethod = HttpMethod.GET)
    public SubscriptionHealth getHealth(@PathParam("topicName") String qualifiedTopicName,
                                        @PathParam("subscriptionName") String subscriptionName) {
        return subscriptionService.getSubscriptionHealth(fromQualifiedName(qualifiedTopicName), subscriptionName);
    }

    @PUT
    @Consumes(APPLICATION_JSON)
    @Path("/{subscriptionName}/state")
    @RolesAllowed({Roles.ADMIN, Roles.TOPIC_OWNER, Roles.SUBSCRIPTION_OWNER})
    @ApiOperation(value = "Update subscription state", httpMethod = HttpMethod.PUT)
    public Response updateState(@PathParam("topicName") String qualifiedTopicName,
                                @PathParam("subscriptionName") String subscriptionName,
                                Subscription.State state,
                                @Context SecurityContext securityContext) {
        subscriptionService.updateSubscriptionState(fromQualifiedName(qualifiedTopicName),
                subscriptionName, state, securityContext.getUserPrincipal().getName());
        return responseStatus(OK);
    }

    @DELETE
    @Path("/{subscriptionName}")
    @RolesAllowed({Roles.ADMIN, Roles.TOPIC_OWNER, Roles.SUBSCRIPTION_OWNER})
    @ApiOperation(value = "Remove subscription", httpMethod = HttpMethod.DELETE)
    public Response remove(@PathParam("topicName") String qualifiedTopicName,
                           @PathParam("subscriptionName") String subscriptionId,
                           @Context SecurityContext securityContext) {
        subscriptionService.removeSubscription(fromQualifiedName(qualifiedTopicName),
                subscriptionId, securityContext.getUserPrincipal().getName());
        return responseStatus(OK);
    }

    @PUT
    @Consumes(APPLICATION_JSON)
    @Path("/{subscriptionName}")
    @RolesAllowed({Roles.ADMIN, Roles.TOPIC_OWNER, Roles.SUBSCRIPTION_OWNER})
    @ApiOperation(value = "Update subscription", httpMethod = HttpMethod.PUT)
    public Response update(@PathParam("topicName") String qualifiedTopicName,
                           @PathParam("subscriptionName") String subscriptionName,
                           PatchData patch,
                           @Context SecurityContext securityContext) {
        subscriptionService.updateSubscription(TopicName.fromQualifiedName(qualifiedTopicName),
                subscriptionName, patch, securityContext.getUserPrincipal().getName());
        return responseStatus(OK);
    }

    @PUT
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @Path("/{subscriptionName}/retransmission")
    @RolesAllowed({Roles.ADMIN, Roles.TOPIC_OWNER, Roles.SUBSCRIPTION_OWNER})
    @ApiOperation(value = "Update subscription offset", httpMethod = HttpMethod.PUT)
    public Response retransmit(@PathParam("topicName") String qualifiedTopicName,
                               @PathParam("subscriptionName") String subscriptionName,
                               @DefaultValue("false") @QueryParam("dryRun") boolean dryRun,
                               @Valid OffsetRetransmissionDate offsetRetransmissionDate) {

        MultiDCOffsetChangeSummary summary = multiDCAwareService.moveOffset(
                topicService.getTopicDetails(TopicName.fromQualifiedName(qualifiedTopicName)),
                subscriptionName,
                offsetRetransmissionDate.getRetransmissionDate().toInstant().toEpochMilli(),
                dryRun);

        return Response.status(OK).entity(summary).build();
    }

    @GET
    @Produces(APPLICATION_JSON)
    @Path("/{subscriptionName}/events/{messageId}/trace")
    public Response getMessageTrace(@PathParam("topicName") String qualifiedTopicName,
                                    @PathParam("subscriptionName") String subscriptionName,
                                    @PathParam("messageId") String messageId) {

        List<MessageTrace> status = subscriptionService.getMessageStatus(qualifiedTopicName, subscriptionName, messageId);

        return Response.status(OK).entity(status).build();
    }

    @GET
    @Produces(APPLICATION_JSON)
    @Path("/{subscriptionName}/consumer-groups")
    public List<ConsumerGroup> describeConsumerGroups(@PathParam("topicName") String qualifiedTopicName,
                                                      @PathParam("subscriptionName") String subscriptionName) {
        Topic topic = topicService.getTopicDetails(fromQualifiedName(qualifiedTopicName));
        return multiDCAwareService.describeConsumerGroups(topic, subscriptionName);
    }

    private Response responseStatus(Response.Status responseStatus) {
        return Response.status(responseStatus).build();
    }

}
