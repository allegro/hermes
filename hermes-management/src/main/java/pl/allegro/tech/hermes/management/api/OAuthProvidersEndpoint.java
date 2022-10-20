package pl.allegro.tech.hermes.management.api;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.allegro.tech.hermes.api.OAuthProvider;
import pl.allegro.tech.hermes.api.PatchData;
import pl.allegro.tech.hermes.management.api.auth.HermesSecurityAwareRequestUser;
import pl.allegro.tech.hermes.management.api.auth.Roles;
import pl.allegro.tech.hermes.management.domain.oauth.OAuthProviderService;

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
import static javax.ws.rs.core.Response.status;

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
    public Response create(OAuthProvider oAuthProvider,
                           @Context ContainerRequestContext requestContext) {
        service.createOAuthProvider(oAuthProvider, new HermesSecurityAwareRequestUser(requestContext));
        return status(Response.Status.CREATED).build();
    }

    @PUT
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @RolesAllowed(Roles.ADMIN)
    @Path("/{oAuthProviderName}")
    @ApiOperation(value = "Update OAuth provider", httpMethod = HttpMethod.PUT)
    public Response update(@PathParam("oAuthProviderName") String oAuthProviderName, PatchData patch,
                           @Context ContainerRequestContext requestContext) {
        service.updateOAuthProvider(oAuthProviderName, patch, new HermesSecurityAwareRequestUser(requestContext));
        return status(Response.Status.OK).build();
    }

    @DELETE
    @Produces(APPLICATION_JSON)
    @RolesAllowed(Roles.ADMIN)
    @Path("/{oAuthProviderName}")
    @ApiOperation(value = "Remove OAuth provider", httpMethod = HttpMethod.DELETE)
    public Response remove(@PathParam("oAuthProviderName") String oAuthProviderName,
                           @Context ContainerRequestContext requestContext) {
        service.removeOAuthProvider(oAuthProviderName, new HermesSecurityAwareRequestUser(requestContext));
        return status(Response.Status.OK).build();
    }
}
