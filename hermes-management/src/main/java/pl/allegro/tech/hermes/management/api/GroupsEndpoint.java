package pl.allegro.tech.hermes.management.api;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.allegro.tech.hermes.api.Group;
import pl.allegro.tech.hermes.api.PatchData;
import pl.allegro.tech.hermes.management.api.auth.HermesSecurityAwareRequestUser;
import pl.allegro.tech.hermes.management.api.auth.ManagementRights;
import pl.allegro.tech.hermes.management.api.auth.Roles;
import pl.allegro.tech.hermes.management.api.validator.ApiPreconditions;
import pl.allegro.tech.hermes.management.domain.group.GroupService;

import java.util.List;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Component
@Path("/groups")
@Api(value = "/groups", description = "Operations on groups")
public class GroupsEndpoint {

    private final GroupService groupService;

    private final ApiPreconditions preconditions;

    private final ManagementRights managementRights;

    @Autowired
    public GroupsEndpoint(GroupService groupService,
                          ApiPreconditions preconditions,
                          ManagementRights managementRights) {
        this.groupService = groupService;
        this.preconditions = preconditions;
        this.managementRights = managementRights;
    }

    @GET
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "List groups", response = List.class, httpMethod = HttpMethod.GET)
    public List<String> list() {
        return groupService.listGroupNames();
    }

    @GET
    @Produces(APPLICATION_JSON)
    @Path("/{groupName}")
    @ApiOperation(value = "Get group details", response = Group.class, httpMethod = HttpMethod.GET)
    public Group get(@PathParam("groupName") String groupName) {
        return groupService.getGroupDetails(groupName);
    }

    @POST
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Create group", response = String.class, httpMethod = HttpMethod.POST)
    @RolesAllowed(Roles.ANY)
    public Response create(Group group, @Context ContainerRequestContext requestContext) {
        preconditions.checkConstraints(group, false);
        groupService.createGroup(
                group,
                new HermesSecurityAwareRequestUser(requestContext),
                managementRights.getGroupCreatorRights(requestContext)
        );
        return Response.status(Response.Status.CREATED).build();
    }

    @PUT
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @Path("/{groupName}")
    @ApiOperation(value = "Update group", response = String.class, httpMethod = HttpMethod.PUT)
    @RolesAllowed(Roles.ADMIN)
    public Response update(@PathParam("groupName") String groupName,
                           PatchData patch,
                           @Context ContainerRequestContext requestContext) {
        groupService.updateGroup(groupName, patch, new HermesSecurityAwareRequestUser(requestContext));
        return responseStatus(Response.Status.NO_CONTENT);
    }

    @DELETE
    @Path("/{groupName}")
    @ApiOperation(value = "Remove group", response = String.class, httpMethod = HttpMethod.DELETE)
    @RolesAllowed(Roles.ANY)
    public Response delete(@PathParam("groupName") String groupName, @Context ContainerRequestContext requestContext) {
        groupService.removeGroup(groupName, new HermesSecurityAwareRequestUser(requestContext));
        return responseStatus(Response.Status.OK);
    }

    private Response responseStatus(Response.Status responseStatus) {
        return Response.status(responseStatus).build();
    }
}
