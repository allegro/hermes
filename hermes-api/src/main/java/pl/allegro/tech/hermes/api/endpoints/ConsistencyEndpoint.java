package pl.allegro.tech.hermes.api.endpoints;

import java.util.List;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

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
