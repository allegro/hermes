package pl.allegro.tech.hermes.management.api;

import org.springframework.beans.factory.annotation.Autowired;
import pl.allegro.tech.hermes.management.infrastructure.monitoring.MonitoringCache;
import pl.allegro.tech.hermes.management.infrastructure.monitoring.TopicSubscription;

import java.util.List;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("monitoring")
public class MonitoringEndpoint {

    private final MonitoringCache monitoringCache;

    @Autowired
    public MonitoringEndpoint(MonitoringCache monitoringCache) {
        this.monitoringCache = monitoringCache;
    }

    @GET
    @Produces(APPLICATION_JSON)
    @Path("/consumergroup")
    public List<TopicSubscription> monitorSubscriptionsPartitions() {
        return monitoringCache.getSubscriptionsWithUnassignedPartitions();
    }


}
