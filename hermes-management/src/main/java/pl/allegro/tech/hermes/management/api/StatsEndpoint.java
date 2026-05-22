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
import pl.allegro.tech.hermes.management.domain.subscription.SubscriptionManagement;
import pl.allegro.tech.hermes.management.domain.topic.TopicManagement;

@Path("stats")
public class StatsEndpoint {
  private final SubscriptionManagement subscriptionManagement;
  private final TopicManagement topicManagement;

  @Autowired
  public StatsEndpoint(
      SubscriptionManagement subscriptionManagement, TopicManagement topicManagement) {
    this.subscriptionManagement = subscriptionManagement;
    this.topicManagement = topicManagement;
  }

  @GET
  @Produces(APPLICATION_JSON)
  @ApiOperation(
      value = "Get topic and subscription stats",
      response = Stats.class,
      httpMethod = HttpMethod.GET)
  public Stats getStats() {
    TopicStats topicStats = topicManagement.getStats();
    SubscriptionStats subscriptionStats = subscriptionManagement.getStats();
    return new Stats(topicStats, subscriptionStats);
  }
}
