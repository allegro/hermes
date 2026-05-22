package pl.allegro.tech.hermes.management.api;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.allegro.tech.hermes.api.Group;
import pl.allegro.tech.hermes.api.Query;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.SubscriptionNameWithMetrics;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.api.TopicNameWithMetrics;
import pl.allegro.tech.hermes.management.domain.group.GroupService;
import pl.allegro.tech.hermes.management.domain.subscription.SubscriptionManagement;
import pl.allegro.tech.hermes.management.domain.topic.TopicManagement;

@Path("query")
@Component
public class QueryEndpoint {

  private final SubscriptionManagement subscriptionManagement;
  private final TopicManagement topicManagement;
  private final GroupService groupService;

  @Autowired
  public QueryEndpoint(
      SubscriptionManagement subscriptionManagement,
      TopicManagement topicManagement,
      GroupService groupService) {
    this.subscriptionManagement = subscriptionManagement;
    this.topicManagement = topicManagement;
    this.groupService = groupService;
  }

  @POST
  @Consumes(APPLICATION_JSON)
  @Produces(APPLICATION_JSON)
  @Path("/groups")
  public List<Group> queryGroups(Query<Group> query) {
    return groupService.queryGroup(query);
  }

  @POST
  @Consumes(APPLICATION_JSON)
  @Produces(APPLICATION_JSON)
  @Path("/topics")
  public List<Topic> queryTopics(Query<Topic> query) {
    return topicManagement.queryTopic(query);
  }

  @POST
  @Consumes(APPLICATION_JSON)
  @Produces(APPLICATION_JSON)
  @Path("/subscriptions")
  public List<Subscription> querySubscriptions(Query<Subscription> query) {
    return subscriptionManagement.querySubscription(query);
  }

  @POST
  @Consumes(APPLICATION_JSON)
  @Produces(APPLICATION_JSON)
  @Path("/topics/metrics")
  public List<TopicNameWithMetrics> queryTopicsMetrics(Query<TopicNameWithMetrics> query) {
    return topicManagement.queryTopicsMetrics(query);
  }

  @POST
  @Consumes(APPLICATION_JSON)
  @Produces(APPLICATION_JSON)
  @Path("subscriptions/metrics")
  public List<SubscriptionNameWithMetrics> querySubscriptionsMetrics(
      Query<SubscriptionNameWithMetrics> query) {
    return subscriptionManagement.querySubscriptionsMetrics(query);
  }
}
