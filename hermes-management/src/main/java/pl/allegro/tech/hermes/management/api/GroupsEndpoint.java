package pl.allegro.tech.hermes.management.api;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.allegro.tech.hermes.api.Group;
import pl.allegro.tech.hermes.api.PatchData;
import pl.allegro.tech.hermes.management.api.auth.HermesSecurityAwareRequestUser;
import pl.allegro.tech.hermes.management.api.auth.ManagementRights;
import pl.allegro.tech.hermes.management.api.auth.Roles;
import pl.allegro.tech.hermes.management.api.validator.ApiPreconditions;
import pl.allegro.tech.hermes.management.domain.group.GroupService;

@Component
@Path("/groups")
@Api(value = "/groups", description = "Operations on groups")
public class GroupsEndpoint {

  private final GroupService groupService;

  private final ApiPreconditions preconditions;

  private final ManagementRights managementRights;

  @Autowired
  public GroupsEndpoint(
      GroupService groupService,
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
        managementRights.getGroupCreatorRights(requestContext));
    return Response.status(Response.Status.CREATED).build();
  }

  @PUT
  @Consumes(APPLICATION_JSON)
  @Produces(APPLICATION_JSON)
  @Path("/{groupName}")
  @ApiOperation(value = "Update group", response = String.class, httpMethod = HttpMethod.PUT)
  @RolesAllowed(Roles.ADMIN)
  public Response update(
      @PathParam("groupName") String groupName,
      PatchData patch,
      @Context ContainerRequestContext requestContext) {
    groupService.updateGroup(groupName, patch, new HermesSecurityAwareRequestUser(requestContext));
    return responseStatus(Response.Status.NO_CONTENT);
  }

  @DELETE
  @Path("/{groupName}")
  @ApiOperation(value = "Remove group", response = String.class, httpMethod = HttpMethod.DELETE)
  @RolesAllowed(Roles.ANY)
  public Response delete(
      @PathParam("groupName") String groupName, @Context ContainerRequestContext requestContext) {
    groupService.removeGroup(groupName, new HermesSecurityAwareRequestUser(requestContext));
    return responseStatus(Response.Status.OK);
  }

  private Response responseStatus(Response.Status responseStatus) {
    return Response.status(responseStatus).build();
  }
}
