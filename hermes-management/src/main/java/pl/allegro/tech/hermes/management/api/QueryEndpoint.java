package pl.allegro.tech.hermes.management.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.allegro.tech.hermes.api.Group;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.query.Query;
import pl.allegro.tech.hermes.management.domain.group.GroupService;
import pl.allegro.tech.hermes.management.domain.subscription.SubscriptionService;
import pl.allegro.tech.hermes.management.domain.topic.TopicService;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import java.util.List;

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
    @Path("/group")
    public List<Group> queryGroup(Query<Group> query) {
        return groupService.queryGroup(query);
    }

    @POST
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @Path("/subscription")
    public List<Subscription> querySubscription(Query<Subscription> query) {
        return subscriptionService.querySubscription(query);
    }

    @POST
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @Path("/topic")
    public List<Topic> queryTopic(Query<Topic> query) {
        return topicService.queryTopic(query);
    }
}


