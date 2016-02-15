package pl.allegro.tech.hermes.test.helper.endpoint;

import com.jayway.awaitility.Duration;
import pl.allegro.tech.hermes.api.BatchSubscriptionPolicy;
import pl.allegro.tech.hermes.api.ContentType;
import pl.allegro.tech.hermes.api.EndpointAddress;
import pl.allegro.tech.hermes.api.Group;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.api.TopicName;

import javax.ws.rs.core.Response;

import static com.jayway.awaitility.Awaitility.waitAtMost;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.OK;
import static org.assertj.core.api.Assertions.assertThat;
import static pl.allegro.tech.hermes.api.BatchSubscriptionPolicy.Builder.batchSubscriptionPolicy;
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

    public Topic createTopic(String group, String topic) {
        Topic created = topic()
                .applyDefaults()
                .withName(group, topic)
                .withRetentionTime(1000)
                .withDescription("Test topic")
                .build();

        createTopic(created);
        return created;
    }

    public Topic createTopic(Topic topic) {
        if (endpoints.findTopics(topic, topic.isTrackingEnabled()).contains(topic.getQualifiedName())) {
            return topic;
        }
        assertThat(endpoints.topic().create(topic).getStatus()).isEqualTo(CREATED.getStatusCode());

        wait.untilTopicCreated(topic);
        return topic;
    }

    public Subscription createSubscription(Topic topic, String subscriptionName, String endpoint) {
        return createSubscription(topic, subscriptionName, endpoint, ContentType.JSON);
    }

    public Subscription createSubscription(Topic topic, String subscriptionName, String endpoint, ContentType contentType) {
        Subscription subscription = subscription()
                .applyDefaults()
                .withName(subscriptionName)
                .withTopicName(topic.getName())
                .withEndpoint(of(endpoint))
                .withSupportTeam("team")
                .withContentType(contentType)
                .withSubscriptionPolicy(subscriptionPolicy().applyDefaults().build())
                .build();

        return createSubscription(topic, subscription);
    }

    public Subscription createSubscription(Topic topic, Subscription subscription) {
        if (endpoints.findSubscriptions(topic.getName().getGroupName(), topic.getName().getName(), subscription.isTrackingEnabled()).contains(subscription.getName())) {
            return subscription;
        }

        assertThat(endpoints.subscription().create(topic.getQualifiedName(), subscription).getStatus()).isEqualTo(CREATED.getStatusCode());

        wait.untilSubscriptionCreated(topic, subscription);
        return subscription;
    }

    public Topic buildTopic(String group, String topic) {
        createGroup(group);
        return createTopic(group, topic);
    }

    public Topic buildTopic(Topic topic) {
        createGroup(topic.getName().getGroupName());
        return createTopic(topic);
    }

    public Response suspendSubscription(Topic topic, String subscription) {
        return endpoints.subscription().updateState(topic.getQualifiedName(), subscription, Subscription.State.SUSPENDED);
    }

    public Response activateSubscription(Topic topic, String subscription) {
        return endpoints.subscription().updateState(topic.getQualifiedName(), subscription, Subscription.State.ACTIVE);
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

    public void updateTopic(String group, String topic, Topic updated) {
        updateTopic(new TopicName(group, topic), updated);
    }

    public void updateTopic(TopicName topicName, Topic updated) {
        assertThat(endpoints.topic().update(topicName.qualifiedName(), updated).getStatus()).isEqualTo(OK.getStatusCode());

        waitAtMost(Duration.ONE_MINUTE).until(() -> {
            return endpoints.topic().get(topicName.qualifiedName()).equals(updated);
        });
    }

    public void createBatchSubscription(Topic topic, String endpoint, int messageTtl, int messageBackoff, int batchSize, int batchTime, int batchVolume, boolean retryOnClientErrors) {
        BatchSubscriptionPolicy policy = batchSubscriptionPolicy()
                .applyDefaults()
                .withMessageTtl(messageTtl)
                .withMessageBackoff(messageBackoff)
                .withBatchSize(batchSize)
                .withBatchTime(batchTime)
                .withBatchVolume(batchVolume)
                .withClientErrorRetry(retryOnClientErrors)
                .withRequestTimeout(100)
                .build();

        createBatchSubscription(topic, endpoint, policy);
    }

    public void createBatchSubscription(Topic topic, String endpoint, BatchSubscriptionPolicy policy) {
        Subscription subscription = subscription()
                .applyDefaults()
                .withContentType(ContentType.JSON)
                .withSupportTeam("supportTeam")
                .withSubscriptionPolicy(policy)
                .withName("batchSubscription")
                .withTopicName(topic.getQualifiedName())
                .withEndpoint(new EndpointAddress(endpoint))
                .build();

        createSubscription(topic, subscription);
    }
}