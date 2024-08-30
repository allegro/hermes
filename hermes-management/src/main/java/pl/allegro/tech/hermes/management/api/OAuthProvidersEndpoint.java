package pl.allegro.tech.hermes.management.api;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static jakarta.ws.rs.core.Response.status;

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
import pl.allegro.tech.hermes.api.OAuthProvider;
import pl.allegro.tech.hermes.api.PatchData;
import pl.allegro.tech.hermes.management.api.auth.HermesSecurityAwareRequestUser;
import pl.allegro.tech.hermes.management.api.auth.Roles;
import pl.allegro.tech.hermes.management.domain.oauth.OAuthProviderService;

@Component
@Path("/oauth/providers")
@Api(value = "/oauth/providers", description = "Operations on OAuth providers")
public class OAuthProvidersEndpoint {

  private final OAuthProviderService service;

  @Autowired
  public OAuthProvidersEndpoint(OAuthProviderService service) {
    this.service = service;
  }

  @GET
  @Produces(APPLICATION_JSON)
  @ApiOperation(value = "List OAuth providers", httpMethod = HttpMethod.GET)
  public List<String> list() {
    return service.listOAuthProviderNames();
  }

  @GET
  @Produces(APPLICATION_JSON)
  @Path("/{oAuthProviderName}")
  @ApiOperation(value = "OAuth provider details", httpMethod = HttpMethod.GET)
  public OAuthProvider get(@PathParam("oAuthProviderName") String oAuthProviderName) {
    return service.getOAuthProviderDetails(oAuthProviderName);
  }

  @POST
  @Consumes(APPLICATION_JSON)
  @Produces(APPLICATION_JSON)
  @RolesAllowed(Roles.ADMIN)
  @ApiOperation(value = "Create OAuth provider", httpMethod = HttpMethod.POST)
  public Response create(
      OAuthProvider oAuthProvider, @Context ContainerRequestContext requestContext) {
    service.createOAuthProvider(oAuthProvider, new HermesSecurityAwareRequestUser(requestContext));
    return status(Response.Status.CREATED).build();
  }

  @PUT
  @Consumes(APPLICATION_JSON)
  @Produces(APPLICATION_JSON)
  @RolesAllowed(Roles.ADMIN)
  @Path("/{oAuthProviderName}")
  @ApiOperation(value = "Update OAuth provider", httpMethod = HttpMethod.PUT)
  public Response update(
      @PathParam("oAuthProviderName") String oAuthProviderName,
      PatchData patch,
      @Context ContainerRequestContext requestContext) {
    service.updateOAuthProvider(
        oAuthProviderName, patch, new HermesSecurityAwareRequestUser(requestContext));
    return status(Response.Status.OK).build();
  }

  @DELETE
  @Produces(APPLICATION_JSON)
  @RolesAllowed(Roles.ADMIN)
  @Path("/{oAuthProviderName}")
  @ApiOperation(value = "Remove OAuth provider", httpMethod = HttpMethod.DELETE)
  public Response remove(
      @PathParam("oAuthProviderName") String oAuthProviderName,
      @Context ContainerRequestContext requestContext) {
    service.removeOAuthProvider(
        oAuthProviderName, new HermesSecurityAwareRequestUser(requestContext));
    return status(Response.Status.OK).build();
  }
}
