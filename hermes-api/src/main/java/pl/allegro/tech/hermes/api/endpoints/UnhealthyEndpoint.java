package pl.allegro.tech.hermes.api.endpoints;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;

@Path("unhealthy")
public interface UnhealthyEndpoint {

    @GET
    @Produces({APPLICATION_JSON, TEXT_PLAIN})
    @Path("/")
    Response listUnhealthy(@QueryParam("ownerSourceName") String ownerSourceName,
                           @QueryParam("ownerId") String id,
                           @QueryParam("respectMonitoringSeverity") boolean respectMonitoringSeverity);
}
