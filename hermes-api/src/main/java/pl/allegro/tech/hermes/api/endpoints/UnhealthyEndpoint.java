package pl.allegro.tech.hermes.api.endpoints;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;

import java.util.List;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static jakarta.ws.rs.core.MediaType.TEXT_PLAIN;

@Path("unhealthy")
public interface UnhealthyEndpoint {

    @GET
    @Produces({APPLICATION_JSON, TEXT_PLAIN})
    @Path("/")
    Response listUnhealthy(@QueryParam("ownerSourceName") String ownerSourceName,
                           @QueryParam("ownerId") String id,
                           @QueryParam("respectMonitoringSeverity") boolean respectMonitoringSeverity,
                           @QueryParam("subscriptionNames") List<String> subscriptionNames,
                           @QueryParam("qualifiedTopicNames") List<String> qualifiedTopicNames);
}
