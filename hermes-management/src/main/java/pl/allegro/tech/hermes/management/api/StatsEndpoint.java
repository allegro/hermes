package pl.allegro.tech.hermes.management.api;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import org.springframework.beans.factory.annotation.Autowired;
import pl.allegro.tech.hermes.api.Stats;
import pl.allegro.tech.hermes.api.SubscriptionStats;
import pl.allegro.tech.hermes.api.TopicStats;
import pl.allegro.tech.hermes.management.domain.subscription.SubscriptionService;
import pl.allegro.tech.hermes.management.domain.topic.TopicService;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("stats")
public class StatsEndpoint {
    private final SubscriptionService subscriptionService;
    private final TopicService topicService;

    @Autowired
    public StatsEndpoint(SubscriptionService subscriptionService, TopicService topicService) {
        this.subscriptionService = subscriptionService;
        this.topicService = topicService;
    }

    @GET
    @Produces(APPLICATION_JSON)
    public Stats getStats() {
        TopicStats topicStats = topicService.getStats();
        SubscriptionStats subscriptionStats = subscriptionService.getStats();
        return new Stats(
                topicStats,
                subscriptionStats
        );
    }

}
