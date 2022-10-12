package pl.allegro.tech.hermes.api.endpoints;

import pl.allegro.tech.hermes.api.Owner;

import java.util.List;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("owners")
public interface OwnerEndpoint {

    @GET
    @Produces(APPLICATION_JSON)
    @Path("/sources/{source}")
    List<Owner> search(@PathParam("source") String source, @QueryParam("search") String searchString);

    @GET
    @Produces(APPLICATION_JSON)
    @Path("/sources/{source}/{id}")
    Owner get(@PathParam("source") String source, @PathParam("id") String id);

    @GET
    @Path("/sources")
    @Produces(APPLICATION_JSON)
    List<String> listSources();
}
