package pl.allegro.tech.hermes.api.endpoints;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import pl.allegro.tech.hermes.api.TopicAndSubscription;

import java.util.List;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("monitoring")
public interface MonitoringEndpoint {

    @GET
    @Produces(APPLICATION_JSON)
    @Path("/consumer-groups")
    List<TopicAndSubscription> monitorConsumerGroups();
}
