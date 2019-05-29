package pl.allegro.tech.hermes.api.endpoints;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("mode")
public interface ModeEndpoint {

    @GET
    @Produces(APPLICATION_JSON)
    String getMode();

    @POST
    @Produces(APPLICATION_JSON)
    Response setMode(@QueryParam("mode") String mode);
}
