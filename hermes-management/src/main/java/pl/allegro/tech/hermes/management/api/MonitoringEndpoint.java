package pl.allegro.tech.hermes.management.api;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import org.springframework.beans.factory.annotation.Autowired;
import pl.allegro.tech.hermes.api.TopicAndSubscription;
import pl.allegro.tech.hermes.management.infrastructure.monitoring.MonitoringCache;

import java.util.List;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("monitoring")
public class MonitoringEndpoint {

    private final MonitoringCache monitoringCache;

    @Autowired
    public MonitoringEndpoint(MonitoringCache monitoringCache) {
        this.monitoringCache = monitoringCache;
    }

    @GET
    @Produces(APPLICATION_JSON)
    @Path("/consumer-groups")
    public List<TopicAndSubscription> monitorConsumerGroups() {
        return monitoringCache.getSubscriptionsWithUnassignedPartitions();
    }
}
