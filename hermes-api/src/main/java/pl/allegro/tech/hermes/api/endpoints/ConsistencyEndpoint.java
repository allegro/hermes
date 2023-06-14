package pl.allegro.tech.hermes.api.endpoints;

import java.util.List;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("consistency")
public interface ConsistencyEndpoint {

    @GET
    @Produces({APPLICATION_JSON})
    @Path("/inconsistencies/groups")
    Response listInconsistentGroups(@QueryParam("groupNames") List<String> groupNames);

    @GET
    @Produces({APPLICATION_JSON})
    @Path("/groups")
    Response listAllGroups();
}
