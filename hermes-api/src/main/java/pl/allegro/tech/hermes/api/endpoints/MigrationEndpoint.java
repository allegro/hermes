package pl.allegro.tech.hermes.api.endpoints;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.List;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("migrations")
public interface MigrationEndpoint {

    @GET
    @Produces(APPLICATION_JSON)
    List<String> list();

    @POST
    @Produces(APPLICATION_JSON)
    @Path("/{name}")
    Response execute(@PathParam("name") String name,
                     @QueryParam("source") String sourceName,
                     @QueryParam("override") boolean overrideOwners);

}
