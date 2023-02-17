package pl.allegro.tech.hermes.api.endpoints;

import pl.allegro.tech.hermes.api.TopicAndSubscription;

import java.util.List;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("monitoring")
public interface MonitoringEndpoint {

    @GET
    @Produces(APPLICATION_JSON)
    @Path("/consumer-groups")
    List<TopicAndSubscription> monitorConsumerGroups();
}
