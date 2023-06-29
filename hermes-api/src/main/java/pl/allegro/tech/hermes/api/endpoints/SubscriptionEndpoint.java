package pl.allegro.tech.hermes.api.endpoints;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;
import pl.allegro.tech.hermes.api.ConsumerGroup;
import pl.allegro.tech.hermes.api.OffsetRetransmissionDate;
import pl.allegro.tech.hermes.api.PatchData;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.SubscriptionHealth;
import pl.allegro.tech.hermes.api.SubscriptionMetrics;

import java.util.List;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("topics/{topicName}/subscriptions")
public interface SubscriptionEndpoint {
    @GET
    @Produces(APPLICATION_JSON)
    List<String> list(@PathParam("topicName") String qualifiedTopicName,
                      @DefaultValue("false") @QueryParam("tracked") boolean tracked);

    @POST
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @Path("/query")
    List<String> queryList(@PathParam("topicName") String qualifiedTopicName,
                           String query);

    @POST
    @Consumes(APPLICATION_JSON)
    Response create(@PathParam("topicName") String qualifiedTopicName,
                    Subscription subscription);

    @GET
    @Produces(APPLICATION_JSON)
    @Path("/{subscriptionName}")
    Subscription get(@PathParam("topicName") String qualifiedTopicName,
                     @PathParam("subscriptionName") String subscriptionName);

    @GET
    @Produces(APPLICATION_JSON)
    @Path("/{subscriptionName}/state")
    Subscription.State getState(@PathParam("topicName") String qualifiedTopicName,
                                @PathParam("subscriptionName") String subscriptionName);

    @GET
    @Produces(APPLICATION_JSON)
    @Path("/{subscriptionName}/undelivered/last")
    Response getLatestUndeliveredMessage(@PathParam("topicName") String qualifiedTopicName,
                                         @PathParam("subscriptionName") String subscriptionName);

    @GET
    @Produces(APPLICATION_JSON)
    @Path("/{subscriptionName}/undelivered")
    Response getLatestUndeliveredMessages(@PathParam("topicName") String qualifiedTopicName,
                                          @PathParam("subscriptionName") String subscriptionName);

    @GET
    @Produces(APPLICATION_JSON)
    @Path("/{subscriptionName}/metrics")
    SubscriptionMetrics getMetrics(@PathParam("topicName") String qualifiedTopicName,
                                   @PathParam("subscriptionName") String subscriptionName);

    @GET
    @Produces(APPLICATION_JSON)
    @Path("/{subscriptionName}/health")
    SubscriptionHealth getHealth(@PathParam("topicName") String qualifiedTopicName,
                                 @PathParam("subscriptionName") String subscriptionName);

    @PUT
    @Consumes(APPLICATION_JSON)
    @Path("/{subscriptionName}/state")
    Response updateState(@PathParam("topicName") String qualifiedTopicName,
                         @PathParam("subscriptionName") String subscriptionName,
                         Subscription.State state);

    @DELETE
    @Path("/{subscriptionName}")
    Response remove(@PathParam("topicName") String qualifiedTopicName,
                    @PathParam("subscriptionName") String subscriptionName);

    @PUT
    @Consumes(APPLICATION_JSON)
    @Path("/{subscriptionName}")
    Response update(@PathParam("topicName") String qualifiedTopicName,
                    @PathParam("subscriptionName") String subscriptionName,
                    PatchData patch);

    @PUT
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @Path("/{subscriptionName}/retransmission")
    Response retransmit(@PathParam("topicName") String qualifiedTopicName,
                        @PathParam("subscriptionName") String subscriptionName,
                        @DefaultValue("false") @QueryParam("dryRun") boolean dryRun,
                        OffsetRetransmissionDate offsetRetransmissionDate);

    @POST
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @Path("/{subscriptionName}/moveOffsetsToTheEnd")
    Response moveOffsetsToTheEnd(@PathParam("topicName") String qualifiedTopicName,
                        @PathParam("subscriptionName") String subscriptionName);

    @GET
    @Produces(APPLICATION_JSON)
    @Path("/{subscriptionName}/events/{messageId}/trace")
    Response getMessageTrace(@PathParam("topicName") String qualifiedTopicName,
                             @PathParam("subscriptionName") String subscriptionName,
                             @PathParam("messageId") String messageId);

    @GET
    @Produces(APPLICATION_JSON)
    @Path("/{subscriptionName}/consumer-groups")
    List<ConsumerGroup> describeConsumerGroups(@PathParam("topicName") String qualifiedTopicName,
                                               @PathParam("subscriptionName") String subscriptionName);
}
