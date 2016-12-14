package pl.allegro.tech.hermes.api.endpoints;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import java.util.List;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("maintainers")
public interface MaintainerEndpoint {

    @GET
    @Produces(APPLICATION_JSON)
    @Path("/{source}/{searchString}")
    List<String> get(@PathParam("source") String source, @PathParam("searchString") String searchString);

    @GET
    @Produces(APPLICATION_JSON)
    @Path("/{source}/{searchString}")
    Response getAsResponse(@PathParam("source") String source, @PathParam("searchString") String searchString);
}
