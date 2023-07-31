package pl.allegro.tech.hermes.api.endpoints;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static jakarta.ws.rs.core.MediaType.TEXT_PLAIN;

@Path("mode")
public interface ModeEndpoint {

    @GET
    @Produces(TEXT_PLAIN)
    String getMode();

    @POST
    @Produces(APPLICATION_JSON)
    Response setMode(@QueryParam("mode") String mode);
}
