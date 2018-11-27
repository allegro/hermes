package pl.allegro.tech.hermes.api.endpoints;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import pl.allegro.tech.hermes.api.UnhealthySubscription;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import java.util.List;

@Path("unhealthy")
public interface UnhealthyEndpoint {

    @GET
    @Produces(APPLICATION_JSON)
    @Path("/")
    List<UnhealthySubscription> listUnhealthy(@QueryParam("ownerSourceName") String ownerSourceName,
                                              @QueryParam("ownerId") String id,
                                              @QueryParam("respectMonitoringSeverity") boolean respectMonitoringSeverity);
}
