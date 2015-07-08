package pl.allegro.tech.hermes.test.helper.endpoint;

import com.jayway.awaitility.Duration;
import pl.allegro.tech.hermes.api.Group;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.api.TopicName;

import javax.ws.rs.core.Response;

import static com.jayway.awaitility.Awaitility.waitAtMost;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.OK;
import static org.assertj.core.api.Assertions.assertThat;
import static pl.allegro.tech.hermes.api.EndpointAddress.of;
import static pl.allegro.tech.hermes.api.Subscription.Builder.subscription;
import static pl.allegro.tech.hermes.api.SubscriptionPolicy.Builder.subscriptionPolicy;
import static pl.allegro.tech.hermes.api.Topic.Builder.topic;

public class HermesAPIOperations {

    protected final HermesEndpoints endpoints;
    protected final Waiter wait;

    public HermesAPIOperations(HermesEndpoints endpoints, Waiter wait) {
        this.endpoints = endpoints;
        this.wait = wait;
    }

    public void createGroup(String group) {
        if (endpoints.group().list().contains(group)) {
            return;
        }
        assertThat(endpoints.group().create(Group.from(group)).getStatus()).isEqualTo(CREATED.getStatusCode());

        wait.untilGroupCreated(group);
    }

    public void createTopic(String group, String topic) {
        createTopic(topic().withName(group, topic).withRetentionTime(1000).withDescription("Test topic").build());
    }

    public void createTopic(Topic topic) {
        if (endpoints.findTopics(topic, topic.isTrackingEnabled()).contains(topic.getQualifiedName())) {
            return;
        }
        assertThat(endpoints.topic().create(topic).getStatus()).isEqualTo(CREATED.getStatusCode());

        wait.untilTopicCreated(topic);
    }

    public void createSubscription(String group, String topic, String subscriptionName, String endpoint) {
        Subscription subscription = subscription()
                .applyDefaults()
                .withName(subscriptionName)
                .withEndpoint(of(endpoint))
                .withSubscriptionPolicy(subscriptionPolicy().applyDefaults().build())
                .build();

        createSubscription(group, topic, subscription);
    }

    public void createSubscription(String group, String topic, Subscription subscription) {
        if (endpoints.findSubscriptions(group, topic, subscription.isTrackingEnabled()).contains(subscription.getName())) {
            return;
        }

        assertThat(endpoints.subscription().create(group + "." + topic, subscription).getStatus()).isEqualTo(CREATED.getStatusCode());

        wait.untilSubscriptionCreated(group, topic, subscription);
    }

    public void buildTopic(String group, String topic) {
        createGroup(group);
        createTopic(group, topic);
    }

    public void buildTopic(Topic topic) {
        createGroup(topic.getName().getGroupName());
        createTopic(topic);
    }

    public void buildSubscription(TopicName topicName, String subscription, String httpEndpointUrl) {
        buildSubscription(topicName.getGroupName(), topicName.getName(), subscription, httpEndpointUrl);
    }

    public void buildSubscription(String group, String topic, String subscription, String endpoint) {
        buildTopic(group, topic);
        createSubscription(group, topic, subscription, endpoint);
    }

    public void buildSubscription(TopicName topic, Subscription subscription) {
        buildTopic(topic.getGroupName(), topic.getName());
        createSubscription(topic.getGroupName(), topic.getName(), subscription);
    }

    public Response suspendSubscription(String group, String topic, String subscription) {
        return endpoints.subscription().updateState(group + "." + topic, subscription, Subscription.State.SUSPENDED);
    }

    public Response activateSubscription(String group, String topic, String subscription) {
        return endpoints.subscription().updateState(group + "." + topic, subscription, Subscription.State.ACTIVE);
    }

    public void updateSubscription(String group, String topic, String subscription, Subscription updated) {
        String qualifiedTopicName = group + "." + topic;

        assertThat(endpoints.subscription().update(qualifiedTopicName, subscription, updated).getStatus()).isEqualTo(OK.getStatusCode());

        waitAtMost(Duration.ONE_MINUTE).until(() -> {
            return endpoints.subscription().get(qualifiedTopicName, subscription).equals(updated);
        });
    }

    public Topic getTopic(String group, String topic) {
        return endpoints.topic().get(group + "." + topic);
    }

    public void updateTopic(TopicName topicName, Topic updated) {
        assertThat(endpoints.topic().update(topicName.qualifiedName(), updated).getStatus()).isEqualTo(OK.getStatusCode());

        waitAtMost(Duration.ONE_MINUTE).until(() -> {
            return endpoints.topic().get(topicName.qualifiedName()).equals(updated);
        });
    }


}