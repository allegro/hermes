package pl.allegro.tech.hermes.api.endpoints;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import pl.allegro.tech.hermes.api.OAuthProvider;
import pl.allegro.tech.hermes.api.PatchData;

import java.util.List;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("oauth/providers")
public interface OAuthProviderEndpoint {

    @GET
    @Produces(APPLICATION_JSON)
    List<String> list();

    @POST
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    Response create(OAuthProvider oAuthProvider);

    @PUT
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @Path("/{oAuthProviderName}")
    Response update(@PathParam("oAuthProviderName") String name, PatchData patch);

    @GET
    @Produces(APPLICATION_JSON)
    @Path("/{oAuthProviderName}")
    OAuthProvider get(@PathParam("oAuthProviderName") String oAuthProviderName);

    @DELETE
    @Produces(APPLICATION_JSON)
    @Path("/{oAuthProviderName}")
    Response remove(@PathParam("oAuthProviderName") String oAuthProviderName);
}
