package pl.allegro.tech.hermes.management.api;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.stereotype.Component;
import pl.allegro.tech.hermes.api.SubscriptionConstraints;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.api.TopicConstraints;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.domain.workload.constraints.ConsumersWorkloadConstraints;
import pl.allegro.tech.hermes.management.api.auth.HermesSecurityAwareRequestUser;
import pl.allegro.tech.hermes.management.api.auth.Roles;
import pl.allegro.tech.hermes.management.domain.auth.RequestUser;
import pl.allegro.tech.hermes.management.domain.workload.constraints.WorkloadConstraintsService;

import java.util.List;
import javax.annotation.security.RolesAllowed;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.OK;

@Component
@Path("/workload-constraints")
@Api(value = "/workload-constraints", description = "Operations on workload constraints")
public class WorkloadConstraintsEndpoint {

    private final WorkloadConstraintsService service;

    public WorkloadConstraintsEndpoint(WorkloadConstraintsService service) {
        this.service = service;
    }

    @GET
    @Produces(APPLICATION_JSON)
    @RolesAllowed(Roles.ANY)
    @ApiOperation(value = "All workload constraints", response = List.class, httpMethod = HttpMethod.GET)
    public ConsumersWorkloadConstraints getConsumersWorkloadConstraints() {
        return service.getConsumersWorkloadConstraints();
    }

    @PUT
    @Path("/topic")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @RolesAllowed(Roles.ADMIN)
    @ApiOperation(value = "Create or update topic constraints", response = String.class, httpMethod = HttpMethod.PUT)
    public Response createOrUpdateTopicConstraints(
            @Valid TopicConstraints topicConstraints,
            @Context ContainerRequestContext requestContext) {
        RequestUser requestUser = new HermesSecurityAwareRequestUser(requestContext);
        if (service.constraintsExist(topicConstraints.getTopicName())) {
            service.updateConstraints(topicConstraints.getTopicName(), topicConstraints.getConstraints(), requestUser);
            return Response.status(OK).build();
        } else {
            service.createConstraints(topicConstraints.getTopicName(), topicConstraints.getConstraints(), requestUser);
            return Response.status(CREATED).build();
        }
    }

    @DELETE
    @Path("/topic/{topicName}")
    @RolesAllowed(Roles.ADMIN)
    @ApiOperation(value = "Remove topic constraints", response = String.class, httpMethod = HttpMethod.DELETE)
    public Response deleteTopicConstraints(
            @PathParam("topicName") String topicName,
            @Context ContainerRequestContext requestContext) {
        service.deleteConstraints(TopicName.fromQualifiedName(topicName), new HermesSecurityAwareRequestUser(requestContext));
        return Response.status(OK).build();
    }

    @PUT
    @Path("/subscription")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @RolesAllowed(Roles.ADMIN)
    @ApiOperation(value = "Create or update subscription constraints", response = String.class, httpMethod = HttpMethod.PUT)
    public Response createOrUpdateSubscriptionConstraints(
            @Valid SubscriptionConstraints subscriptionConstraints,
            @Context ContainerRequestContext requestContext) {
        RequestUser requestUser = new HermesSecurityAwareRequestUser(requestContext);
        if (service.constraintsExist(subscriptionConstraints.getSubscriptionName())) {
            service.updateConstraints(subscriptionConstraints.getSubscriptionName(), subscriptionConstraints.getConstraints(), requestUser);
            return Response.status(OK).build();
        } else {
            service.createConstraints(subscriptionConstraints.getSubscriptionName(), subscriptionConstraints.getConstraints(), requestUser);
            return Response.status(CREATED).build();
        }
    }

    @DELETE
    @Path("/subscription/{topicName}/{subscriptionName}")
    @RolesAllowed(Roles.ADMIN)
    @ApiOperation(value = "Remove subscription constraints", response = String.class, httpMethod = HttpMethod.DELETE)
    public Response deleteSubscriptionConstraints(@PathParam("topicName") String topicName,
                                                  @PathParam("subscriptionName") String subscriptionName,
                                                  @Context ContainerRequestContext requestContext) {
        service.deleteConstraints(
                new SubscriptionName(subscriptionName, TopicName.fromQualifiedName(topicName)),
                new HermesSecurityAwareRequestUser(requestContext)
        );
        return Response.status(OK).build();
    }
}
