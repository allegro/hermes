package pl.allegro.tech.hermes.api.endpoints;

import pl.allegro.tech.hermes.api.Maintainer;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.List;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("maintainers")
public interface MaintainerEndpoint {

    @GET
    @Produces(APPLICATION_JSON)
    @Path("/sources/{source}")
    List<Maintainer> search(@PathParam("source") String source, @QueryParam("search") String searchString);

    @GET
    @Produces(APPLICATION_JSON)
    @Path("/sources/{source}")
    Response searchAsResponse(@PathParam("source") String source, @QueryParam("search") String searchString);

    @GET
    @Produces(APPLICATION_JSON)
    @Path("/sources/{source}/{id}")
    Maintainer get(@PathParam("source") String source, @PathParam("id") String id);

    @GET
    @Path("/sources")
    @Produces(APPLICATION_JSON)
    List<String> listSources();
}
