package pl.allegro.tech.hermes.management.api;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

import io.swagger.annotations.ApiOperation;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import org.springframework.beans.factory.annotation.Autowired;
import pl.allegro.tech.hermes.api.Stats;
import pl.allegro.tech.hermes.api.SubscriptionStats;
import pl.allegro.tech.hermes.api.TopicStats;
import pl.allegro.tech.hermes.management.domain.subscription.SubscriptionService;
import pl.allegro.tech.hermes.management.domain.topic.TopicService;

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
  @ApiOperation(
      value = "Get topic and subscription stats",
      response = Stats.class,
      httpMethod = HttpMethod.GET)
  public Stats getStats() {
    TopicStats topicStats = topicService.getStats();
    SubscriptionStats subscriptionStats = subscriptionService.getStats();
    return new Stats(topicStats, subscriptionStats);
  }
}
