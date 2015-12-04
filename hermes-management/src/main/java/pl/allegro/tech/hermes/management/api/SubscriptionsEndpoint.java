package pl.allegro.tech.hermes.management.api;

import com.wordnik.swagger.annotations.ApiOperation;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.beans.factory.annotation.Autowired;
import pl.allegro.tech.hermes.api.MessageTrace;
import pl.allegro.tech.hermes.api.SentMessageTrace;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.SubscriptionMetrics;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.management.api.auth.Roles;
import pl.allegro.tech.hermes.management.api.validator.ApiPreconditions;
import pl.allegro.tech.hermes.management.domain.subscription.SubscriptionService;
import pl.allegro.tech.hermes.management.domain.topic.TopicService;
import pl.allegro.tech.hermes.management.infrastructure.kafka.MultiDCAwareService;
import pl.allegro.tech.hermes.management.infrastructure.kafka.MultiDCOffsetChangeSummary;
import pl.allegro.tech.hermes.management.infrastructure.time.TimeFormatter;

import javax.annotation.security.RolesAllowed;
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
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.OK;
import static pl.allegro.tech.hermes.api.Subscription.Builder.subscription;
import static pl.allegro.tech.hermes.api.TopicName.fromQualifiedName;

@Path("topics/{topicName}/subscriptions")
public class SubscriptionsEndpoint {

    private final SubscriptionService subscriptionService;
    private final TopicService topicService;
    private final ApiPreconditions preconditions;
    private final MultiDCAwareService multiDCAwareService;
    private final TimeFormatter timeFormatter;

    @Autowired
    public SubscriptionsEndpoint(SubscriptionService subscriptionService,
                                 TopicService topicService,
                                 ApiPreconditions preconditions,
                                 MultiDCAwareService multiDCAwareService,
                                 TimeFormatter timeFormatter) {
        this.subscriptionService = subscriptionService;
        this.topicService = topicService;
        this.preconditions = preconditions;
        this.multiDCAwareService = multiDCAwareService;
        this.timeFormatter = timeFormatter;
    }

    @GET
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "List subscriptions", response = List.class, httpMethod = HttpMethod.GET)
    public List<String> list(
            @PathParam("topicName") String qualifiedTopicName,
            @DefaultValue("false") @QueryParam("tracked") boolean tracked) {

        return tracked?
                subscriptionService.listTrackedSubscriptionNames(fromQualifiedName(qualifiedTopicName)) :
                subscriptionService.listSubscriptionNames(fromQualifiedName(qualifiedTopicName));
    }

    @POST
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @Path("/query")
    @ApiOperation(value = "Query subscriptions", response = List.class, httpMethod = HttpMethod.POST)
    public List<String> queryList(
            @PathParam("topicName") String qualifiedTopicName,
            String query) throws IOException {

        return subscriptionService.listFilteredSubscriptionNames(fromQualifiedName(qualifiedTopicName), query);
    }

    @POST
    @Consumes(APPLICATION_JSON)
    @RolesAllowed({Roles.ANY})
    @ApiOperation(value = "Create subscription", httpMethod = HttpMethod.POST)
    public Response create(@PathParam("topicName") String qualifiedTopicName, Subscription subscription) {
        preconditions.checkConstraints(subscription);

        subscriptionService.createSubscription(
                subscription().applyDefaults().applyPatch(subscription).withTopicName(qualifiedTopicName).build());
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

    @PUT
    @Consumes(APPLICATION_JSON)
    @Path("/{subscriptionName}/state")
    @RolesAllowed({Roles.SUBSCRIPTION_OWNER, Roles.GROUP_OWNER, Roles.ADMIN})
    @ApiOperation(value = "Update subscription state", httpMethod = HttpMethod.PUT)
    public Response updateState(@PathParam("topicName") String qualifiedTopicName,
                                @PathParam("subscriptionName") String subscriptionName,
                                Subscription.State state) {
        subscriptionService.updateSubscriptionState(fromQualifiedName(qualifiedTopicName), subscriptionName, state);
        return responseStatus(OK);
    }

    @DELETE
    @Path("/{subscriptionName}")
    @RolesAllowed({Roles.SUBSCRIPTION_OWNER, Roles.GROUP_OWNER, Roles.ADMIN})
    @ApiOperation(value = "Remove subscription", httpMethod = HttpMethod.DELETE)
    public Response remove(@PathParam("topicName") String qualifiedTopicName,
                           @PathParam("subscriptionName") String subscriptionId) {
        subscriptionService.removeSubscription(fromQualifiedName(qualifiedTopicName), subscriptionId);
        return responseStatus(OK);
    }

    @PUT
    @Consumes(APPLICATION_JSON)
    @Path("/{subscriptionName}")
    @RolesAllowed({Roles.SUBSCRIPTION_OWNER, Roles.GROUP_OWNER, Roles.ADMIN})
    @ApiOperation(value = "Update subscription", httpMethod = HttpMethod.PUT)
    public Response update(@PathParam("topicName") String qualifiedTopicName,
                           @PathParam("subscriptionName") String subscriptionName,
                           Subscription subscription) {
        subscriptionService.updateSubscription(
                subscription().withTopicName(qualifiedTopicName).withName(subscriptionName).applyPatch(subscription).build()
        );
        return responseStatus(OK);
    }

    @PUT
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @Path("/{subscriptionName}/retransmission")
    @RolesAllowed({Roles.SUBSCRIPTION_OWNER, Roles.GROUP_OWNER, Roles.ADMIN})
    @ApiOperation(value = "Update subscription offset", httpMethod = HttpMethod.PUT)
    public Response retransmit(@PathParam("topicName") String qualifiedTopicName,
                               @PathParam("subscriptionName") String subscriptionName,
                               @DefaultValue("false") @QueryParam("dryRun") boolean dryRun,
                               @NotEmpty String formattedTime) {

        MultiDCOffsetChangeSummary summary = multiDCAwareService.moveOffset(
                topicService.getTopicDetails(TopicName.fromQualifiedName(qualifiedTopicName)),
                subscriptionName,
                timeFormatter.parse(formattedTime),
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

    private Response responseStatus(Response.Status responseStatus) {
        return Response.status(responseStatus).build();
    }

}
