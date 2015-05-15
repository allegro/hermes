package pl.allegro.tech.hermes.test.helper.endpoint;

import pl.allegro.tech.hermes.api.Group;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.api.TopicName;

import javax.ws.rs.core.Response;

import static pl.allegro.tech.hermes.api.EndpointAddress.of;
import static pl.allegro.tech.hermes.api.Group.Builder.group;
import static pl.allegro.tech.hermes.api.Subscription.Builder.subscription;
import static pl.allegro.tech.hermes.api.SubscriptionPolicy.Builder.subscriptionPolicy;
import static pl.allegro.tech.hermes.api.Topic.Builder.topic;

public class HermesAPIOperations {

    private final HermesEndpoints endpoints;
    private final String adminPassword;

    public HermesAPIOperations(HermesEndpoints endpoints, String adminPassword) {
        this.endpoints = endpoints;
        this.adminPassword = adminPassword;
    }

    public Response createGroup(String group, String supportTeam) {
        return endpoints.group().create(group().withGroupName(group).withSupportTeam(supportTeam).build());
    }

    public Response createGroup(String group) {
        return endpoints.group().create(Group.from(group));
    }

    public Response createTopic(String group, String topic) {
        return endpoints.topic().create(
                topic().withName(group, topic).withRetentionTime(1000).withDescription("Test topic").build());
    }

    public void createTopic(Topic topic) {
        endpoints.topic().create(topic);
    }

    public Response createSubscription(String group, String topic, String subscriptionName, String endpoint) {
        Subscription subscription = subscription()
                .applyDefaults()
                .withName(subscriptionName)
                .withEndpoint(of(endpoint))
                .withSubscriptionPolicy(subscriptionPolicy().applyDefaults().build())
                .build();

        return createSubscription(group, topic, subscription);
    }

    public Response createSubscription(String group, String topic, Subscription subscription) {
        return endpoints.subscription().create(group + "." + topic, subscription);
    }

    public void buildTopic(String group, String topic) {
        createGroup(group, adminPassword);
        createTopic(group, topic);
    }

    public void buildTopic(Topic topic) {
        createGroup(topic.getName().getGroupName(), adminPassword);
        createTopic(topic);
    }

    public void buildSubscription(TopicName topicName, String subscription, String httpEndpointUrl) {
        buildSubscription(topicName.getGroupName(), topicName.getName(), subscription, httpEndpointUrl);
    }

    public void buildSubscription(String group, String topic, String subscription, String endpoint) {
        buildTopic(group, topic);
        createSubscription(group, topic, subscription, endpoint);
    }

    public Response suspendSubscription(String group, String topic, String subscription) {
        return endpoints.subscription().updateState(group + "." + topic, subscription, Subscription.State.SUSPENDED);
    }

    public Response activateSubscription(String group, String topic, String subscription) {
        return endpoints.subscription().updateState(group + "." + topic, subscription, Subscription.State.ACTIVE);
    }

    public Response updateSubscription(String group, String topic, String subscription, Subscription updated) {
        return endpoints.subscription().update(group + "." + topic, subscription, updated);
    }

    public Topic getTopic(String group, String topic) {
        return endpoints.topic().get(group + "." + topic);
    }

    public Subscription getSubscription(String group, String topic, String subscription) {
        return endpoints.subscription().get(group + "." + topic, subscription);
    }

    public void updateTopic(TopicName topicName, Topic updated) {
        endpoints.topic().update(topicName.qualifiedName(), updated);
    }
}
