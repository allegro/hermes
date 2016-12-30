package pl.allegro.tech.hermes.api.endpoints;

import pl.allegro.tech.hermes.api.Maintainer;

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
    @Path("/sources/{source}/{searchString}")
    List<String> search(@PathParam("source") String source, @PathParam("searchString") String searchString);

    @GET
    @Produces(APPLICATION_JSON)
    @Path("/sources/{source}/{searchString}")
    Response searchAsResponse(@PathParam("source") String source, @PathParam("searchString") String searchString);

    @GET
    @Produces(APPLICATION_JSON)
    @Path("/sources/{source}/{id}")
    Maintainer get(@PathParam("source") String source, @PathParam("id") String id);

    @GET
    @Path("/sources")
    @Produces(APPLICATION_JSON)
    List<String> listSources();
}
