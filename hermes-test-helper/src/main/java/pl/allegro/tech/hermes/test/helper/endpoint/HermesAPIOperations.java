package pl.allegro.tech.hermes.test.helper.endpoint;

import pl.allegro.tech.hermes.api.BatchSubscriptionPolicy;
import pl.allegro.tech.hermes.api.ContentType;
import pl.allegro.tech.hermes.api.Group;
import pl.allegro.tech.hermes.api.OAuthProvider;
import pl.allegro.tech.hermes.api.PatchData;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.SubscriptionMode;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.api.TopicWithSchema;
import pl.allegro.tech.hermes.api.helpers.Patch;

import javax.ws.rs.core.Response;

import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.OK;
import static org.assertj.core.api.Assertions.assertThat;
import static pl.allegro.tech.hermes.api.BatchSubscriptionPolicy.Builder.batchSubscriptionPolicy;
import static pl.allegro.tech.hermes.api.SubscriptionPolicy.Builder.subscriptionPolicy;
import static pl.allegro.tech.hermes.api.TopicWithSchema.topicWithSchema;
import static pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder.subscription;
import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.topic;

public class HermesAPIOperations {

    protected final HermesEndpoints endpoints;
    protected final Waiter wait;

    public HermesAPIOperations(HermesEndpoints endpoints, Waiter wait) {
        this.endpoints = endpoints;
        this.wait = wait;
    }

    public void createGroup(String group) {
        createGroup(group, "team");
    }

    public void createGroup(String group, String supportTeam) {
        if (endpoints.group().list().contains(group)) {
            return;
        }
        assertThat(endpoints.group().create(new Group(group, supportTeam)).getStatus()).isEqualTo(CREATED.getStatusCode());

        wait.untilGroupCreated(group);
    }

    public TopicWithSchema createTopic(String group, String topic) {
        Topic created = topic(group, topic)
                .withRetentionTime(1000)
                .withDescription("Test topic")
                .build();
        return createTopic(created);
    }

    public TopicWithSchema createTopic(Topic topic) {
        return createTopic(topic, null);
    }

    public TopicWithSchema createTopic(Topic topic, String schema) {
        return createTopic(topicWithSchema(topic, schema));
    }

    public TopicWithSchema createTopic(TopicWithSchema topicWithSchema) {
        if (endpoints.findTopics(topicWithSchema, topicWithSchema.isTrackingEnabled()).contains(topicWithSchema.getQualifiedName())) {
            return topicWithSchema;
        }
        assertThat(endpoints.topic().create(topicWithSchema).getStatus()).isEqualTo(CREATED.getStatusCode());

        wait.untilTopicCreated(topicWithSchema);
        return topicWithSchema;
    }

    public void saveSchema(Topic topic, String schema) {
        Response response = endpoints.schema().save(topic.getQualifiedName(), false, schema);
        assertThat(response.getStatus()).isEqualTo(CREATED.getStatusCode());

        wait.untilSchemaCreated(topic);
    }

    public Subscription createSubscription(Topic topic, String subscriptionName, String endpoint) {
        return createSubscription(topic, subscriptionName, endpoint, ContentType.JSON);
    }

    public Subscription createSubscription(Topic topic, String subscriptionName, String endpoint, Subscription.State state) {
        return createSubscription(topic, subscriptionName, endpoint, ContentType.JSON, SubscriptionMode.ANYCAST, state);
    }

    public Subscription createSubscription(Topic topic, String subscriptionName, String endpoint, ContentType contentType) {
        return createSubscription(topic, subscriptionName, endpoint, contentType, SubscriptionMode.ANYCAST, Subscription.State.PENDING);
    }

    public Subscription createBroadcastSubscription(Topic topic, String subscriptionName, String endpoint) {
        return createSubscription(topic, subscriptionName, endpoint, ContentType.JSON, SubscriptionMode.BROADCAST, Subscription.State.PENDING);
    }

    public Subscription createSubscription(Topic topic,
                                           String subscriptionName,
                                           String endpoint,
                                           ContentType contentType,
                                           SubscriptionMode mode,
                                           Subscription.State state) {
        Subscription subscription = subscription(topic, subscriptionName)
                .withEndpoint(endpoint)
                .withContentType(contentType)
                .withSubscriptionPolicy(subscriptionPolicy().applyDefaults().build())
                .withMode(mode)
                .withState(state)
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

    public TopicWithSchema buildTopicWithSchema(TopicWithSchema topic) {
        createGroup(topic.getName().getGroupName());
        return createTopic(topic);
    }

    public TopicWithSchema buildTopic(Topic topic) {
        createGroup(topic.getName().getGroupName());
        return createTopic(topic);
    }

    public Response suspendSubscription(Topic topic, String subscription) {
        return endpoints.subscription().updateState(topic.getQualifiedName(), subscription, Subscription.State.SUSPENDED);
    }

    public Response activateSubscription(Topic topic, String subscription) {
        return endpoints.subscription().updateState(topic.getQualifiedName(), subscription, Subscription.State.ACTIVE);
    }

    public void updateSubscription(String group, String topic, String subscription, PatchData patch) {
        String qualifiedTopicName = group + "." + topic;

        assertThat(endpoints.subscription().update(qualifiedTopicName, subscription, patch).getStatus()).isEqualTo(OK.getStatusCode());
    }

    public Topic getTopic(String group, String topic) {
        return endpoints.topic().get(group + "." + topic);
    }

    public void updateTopic(String group, String topic, PatchData patch) {
        updateTopic(new TopicName(group, topic), patch);
    }

    public void updateTopic(TopicName topicName, PatchData patch) {
        Topic beforeUpdate = endpoints.topic().get(topicName.qualifiedName());
        Topic reference = Patch.apply(beforeUpdate, patch);

        assertThat(endpoints.topic().update(topicName.qualifiedName(), patch).getStatus()).isEqualTo(OK.getStatusCode());
        wait.untilTopicUpdated(reference);
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
        Subscription subscription = subscription(topic, "batchSubscription")
                .withEndpoint(endpoint)
                .withContentType(ContentType.JSON)
                .withSubscriptionPolicy(policy)
                .build();

        createSubscription(topic, subscription);
    }

    public void createOAuthProvider(OAuthProvider oAuthProvider) {
        if (endpoints.oAuthProvider().list().contains(oAuthProvider.getName())) {
            return;
        }
        assertThat(endpoints.oAuthProvider().create(oAuthProvider).getStatus()).isEqualTo(CREATED.getStatusCode());

        wait.untilOAuthProviderCreated(oAuthProvider.getName());
    }

}