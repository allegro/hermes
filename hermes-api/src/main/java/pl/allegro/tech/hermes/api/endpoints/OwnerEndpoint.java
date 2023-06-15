package pl.allegro.tech.hermes.api.endpoints;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import pl.allegro.tech.hermes.api.Owner;

import java.util.List;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

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
