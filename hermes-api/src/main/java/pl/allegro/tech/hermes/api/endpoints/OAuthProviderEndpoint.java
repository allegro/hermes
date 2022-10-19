package pl.allegro.tech.hermes.api.endpoints;

import pl.allegro.tech.hermes.api.OAuthProvider;
import pl.allegro.tech.hermes.api.PatchData;

import java.util.List;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

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
