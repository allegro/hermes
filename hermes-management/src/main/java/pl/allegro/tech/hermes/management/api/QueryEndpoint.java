package pl.allegro.tech.hermes.management.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.allegro.tech.hermes.api.*;
import pl.allegro.tech.hermes.management.domain.group.GroupService;
import pl.allegro.tech.hermes.management.domain.subscription.SubscriptionService;
import pl.allegro.tech.hermes.management.domain.topic.TopicService;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import java.util.*;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("query")
@Component
public class QueryEndpoint {

    private final SubscriptionService subscriptionService;
    private final TopicService topicService;
    private final GroupService groupService;

    @Autowired
    public QueryEndpoint(SubscriptionService subscriptionService, TopicService topicService, GroupService groupService) {
        this.subscriptionService = subscriptionService;
        this.topicService = topicService;
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
        return topicService.queryTopic(query);
    }

    @POST
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @Path("/subscriptions")
    public List<Subscription> querySubscriptions(Query<Subscription> query) {
        return subscriptionService.querySubscription(query);
    }

    @POST
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @Path("/topics/metrics")
    public List<TopicNameWithMetrics> queryTopicsMetrics(Query<TopicNameWithMetrics> query) {
        return topicService.getTopicsMetrics(query);
    }
}