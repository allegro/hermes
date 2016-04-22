package pl.allegro.tech.hermes.test.helper.builder;

import pl.allegro.tech.hermes.api.BatchSubscriptionPolicy;
import pl.allegro.tech.hermes.api.ContentType;
import pl.allegro.tech.hermes.api.DeliveryType;
import pl.allegro.tech.hermes.api.EndpointAddress;
import pl.allegro.tech.hermes.api.MessageFilterSpecification;
import pl.allegro.tech.hermes.api.MonitoringDetails;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.api.SubscriptionPolicy;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.api.TopicName;

import java.util.ArrayList;
import java.util.List;

public class SubscriptionBuilder {

    private final TopicName topicName;

    private final String name;

    private Subscription.State state = Subscription.State.PENDING;

    private EndpointAddress endpoint = EndpointAddress.of("http://localhost:12345");

    private ContentType contentType = ContentType.JSON;

    private String description = "description";

    private SubscriptionPolicy serialSubscriptionPolicy = new SubscriptionPolicy(100, 10, 1000, false, 100);

    private BatchSubscriptionPolicy batchSubscriptionPolicy;

    private boolean trackingEnabled = false;

    private String supportTeam = "team";

    private String contact = "contact";

    private MonitoringDetails monitoringDetails = MonitoringDetails.EMPTY;

    private DeliveryType deliveryType = DeliveryType.SERIAL;

    private List<MessageFilterSpecification> filters = new ArrayList<>();

    private SubscriptionBuilder(TopicName topicName, String subscriptionName, EndpointAddress endpoint) {
        this.topicName = topicName;
        this.name = subscriptionName;
        this.endpoint = endpoint;
    }

    private SubscriptionBuilder(TopicName topicName, String subscriptionName) {
        this.topicName = topicName;
        this.name = subscriptionName;
    }

    public static SubscriptionBuilder subscription(SubscriptionName subscriptionName) {
        return new SubscriptionBuilder(subscriptionName.getTopicName(), subscriptionName.getName());
    }

    public static SubscriptionBuilder subscription(TopicName topicName, String subscriptionName) {
        return new SubscriptionBuilder(topicName, subscriptionName);
    }

    public static SubscriptionBuilder subscription(Topic topic, String subscriptionName) {
        return new SubscriptionBuilder(topic.getName(), subscriptionName);
    }

    public static SubscriptionBuilder subscription(TopicName topicName, String subscriptionName, EndpointAddress endpoint) {
        return new SubscriptionBuilder(topicName, subscriptionName, endpoint);
    }

    public static SubscriptionBuilder subscription(String topicQualifiedName, String subscriptionName) {
        return new SubscriptionBuilder(TopicName.fromQualifiedName(topicQualifiedName), subscriptionName);
    }

    public static SubscriptionBuilder subscription(String topicQualifiedName, String subscriptionName, String endpoint) {
        return subscription(TopicName.fromQualifiedName(topicQualifiedName), subscriptionName, EndpointAddress.of(endpoint));
    }

    public static SubscriptionBuilder subscription(String topicQualifiedName, String subscriptionName, EndpointAddress endpoint) {
        return subscription(TopicName.fromQualifiedName(topicQualifiedName), subscriptionName, endpoint);
    }

    public Subscription build() {
        if (deliveryType == DeliveryType.SERIAL) {
            return Subscription.createSerialSubscription(
                    topicName, name, endpoint, state, description,
                    serialSubscriptionPolicy,
                    trackingEnabled, supportTeam, contact, monitoringDetails, contentType, filters
            );
        } else {
            return Subscription.createBatchSubscription(
                    topicName, name, endpoint, state, description,
                    batchSubscriptionPolicy,
                    trackingEnabled, supportTeam, contact, monitoringDetails, contentType, filters
            );
        }
    }

    public SubscriptionBuilder withEndpoint(EndpointAddress endpoint) {
        this.endpoint = endpoint;
        return this;
    }

    public SubscriptionBuilder withEndpoint(String endpoint) {
        this.endpoint = EndpointAddress.of(endpoint);
        return this;
    }

    public SubscriptionBuilder withState(Subscription.State state) {
        this.state = state;
        return this;
    }

    public SubscriptionBuilder withDescription(String description) {
        this.description = description;
        return this;
    }

    public SubscriptionBuilder withSubscriptionPolicy(BatchSubscriptionPolicy subscriptionPolicy) {
        this.batchSubscriptionPolicy = subscriptionPolicy;
        this.deliveryType = DeliveryType.BATCH;
        return this;
    }

    public SubscriptionBuilder withSubscriptionPolicy(SubscriptionPolicy subscriptionPolicy) {
        this.serialSubscriptionPolicy = subscriptionPolicy;
        this.deliveryType = DeliveryType.SERIAL;
        return this;
    }

    public SubscriptionBuilder withRequestTimeout(int timeout) {
        SubscriptionPolicy policy = this.serialSubscriptionPolicy;
        this.serialSubscriptionPolicy = SubscriptionPolicy.Builder.subscriptionPolicy().withRate(policy.getRate())
                .withMessageTtl(policy.getMessageTtl()).withMessageBackoff(policy.getMessageBackoff())
                .withRequestTimeout(timeout).build();
        return this;
    }

    public SubscriptionBuilder withTrackingEnabled(boolean trackingEnabled) {
        this.trackingEnabled = trackingEnabled;
        return this;
    }

    public SubscriptionBuilder withSupportTeam(String supportTeam) {
        this.supportTeam = supportTeam;
        return this;
    }

    public SubscriptionBuilder withContact(String contact) {
        this.contact = contact;
        return this;
    }

    public SubscriptionBuilder withMonitoringDetails(MonitoringDetails monitoringDetails) {
        this.monitoringDetails = monitoringDetails;
        return this;
    }

    public SubscriptionBuilder withDeliveryType(DeliveryType deliveryType) {
        this.deliveryType = deliveryType;
        return this;
    }

    public SubscriptionBuilder withContentType(ContentType contentType) {
        this.contentType = contentType;
        return this;
    }

    public SubscriptionBuilder withFilter(MessageFilterSpecification filter) {
        this.filters.add(filter);
        return this;
    }
}
