package pl.allegro.tech.hermes.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Joiner;
import org.hibernate.validator.constraints.NotEmpty;
import pl.allegro.tech.hermes.api.helpers.Patch;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.util.Map;
import java.util.Objects;

import static pl.allegro.tech.hermes.api.BatchSubscriptionPolicy.Builder.batchSubscriptionPolicy;
import static pl.allegro.tech.hermes.api.constraints.Names.ALLOWED_NAME_REGEX;
import static pl.allegro.tech.hermes.api.helpers.Replacer.replaceInAll;

public class Subscription {

    @Valid
    private TopicName topicName;

    @NotEmpty
    @Pattern(regexp = ALLOWED_NAME_REGEX)
    private String name;

    private State state = State.PENDING;

    @NotNull
    @Valid
    private EndpointAddress endpoint;

    private ContentType contentType = ContentType.JSON;

    private String description;

    @Valid
    private SubscriptionPolicy serialSubscriptionPolicy;

    @Valid
    private BatchSubscriptionPolicy batchSubscriptionPolicy;

    private boolean trackingEnabled;

    @NotNull
    private String supportTeam;

    private String contact;

    private DeliveryType deliveryType = DeliveryType.SERIAL;

    public enum State {
        PENDING, ACTIVE, SUSPENDED
    }

    private Subscription() {
    }

    public Subscription(TopicName topicName, String name, EndpointAddress endpoint, String description,
                        Map<String, Object> subscriptionPolicy, boolean trackingEnabled, String supportTeam, String contact,
                        ContentType contentType, DeliveryType deliveryType) {
        this.topicName = topicName;
        this.name = name;
        this.description = description;
        this.endpoint = endpoint;
        this.trackingEnabled = trackingEnabled;
        this.supportTeam = supportTeam;
        this.contact = contact;
        this.contentType = contentType == null ? ContentType.JSON : contentType;
        this.deliveryType = deliveryType;
        this.batchSubscriptionPolicy = this.deliveryType == DeliveryType.BATCH ? createBatchPolicy(subscriptionPolicy) : null;
        this.serialSubscriptionPolicy = this.deliveryType == DeliveryType.SERIAL ? createSerialPolicy(subscriptionPolicy) : null;
    }

    @JsonCreator
    public Subscription(@JsonProperty("topicName") String topicName,
                        @JsonProperty("name") String name,
                        @JsonProperty("endpoint") EndpointAddress endpoint,
                        @JsonProperty("description") String description,
                        @JsonProperty("subscriptionPolicy") Map<String, Object> subscriptionPolicy,
                        @JsonProperty("trackingEnabled") boolean trackingEnabled,
                        @JsonProperty("supportTeam") String supportTeam,
                        @JsonProperty("contact") String contact,
                        @JsonProperty("contentType") ContentType contentType,
                        @JsonProperty("deliveryType") DeliveryType deliveryType) {
        this(TopicName.fromQualifiedName(topicName), name, endpoint, description, subscriptionPolicy,
                trackingEnabled, supportTeam, contact, contentType, deliveryType);
    }

    private BatchSubscriptionPolicy createBatchPolicy(Map<String, Object> subscriptionPolicy) {
        return batchSubscriptionPolicy().applyPatch(subscriptionPolicy).build();
    }

    private SubscriptionPolicy createSerialPolicy(Map<String, Object> subscriptionPolicy) {
        return SubscriptionPolicy.Builder.subscriptionPolicy().applyPatch(subscriptionPolicy).build();
    }

    @Override
    public int hashCode() {
        return Objects.hash(endpoint, topicName, name, description, serialSubscriptionPolicy, batchSubscriptionPolicy, contentType);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final Subscription other = (Subscription) obj;

        return Objects.equals(this.endpoint, other.endpoint)
                && Objects.equals(this.topicName, other.topicName)
                && Objects.equals(this.name, other.name)
                && Objects.equals(this.description, other.description)
                && Objects.equals(this.serialSubscriptionPolicy, other.serialSubscriptionPolicy)
                && Objects.equals(this.batchSubscriptionPolicy, other.batchSubscriptionPolicy)
                && Objects.equals(this.trackingEnabled, other.trackingEnabled)
                && Objects.equals(this.supportTeam, other.supportTeam)
                && Objects.equals(this.contact, other.contact)
                && Objects.equals(this.contentType, other.contentType);
    }

    public SubscriptionName toSubscriptionName() {
        return new SubscriptionName(name, topicName);
    }

    @JsonIgnore
    public String getId() {
        return getId(getTopicName(), getName());
    }

    public static String getId(TopicName topicName, String subscriptionName) {
        return Joiner.on("_").join(replaceInAll("_", "__", topicName.getGroupName(), topicName.getName(), subscriptionName));
    }

    public static Subscription fromSubscriptionName(SubscriptionName subscriptionName) {
        return Builder.subscription().withTopicName(subscriptionName.getTopicName()).withName(subscriptionName.getName()).build();
    }

    public EndpointAddress getEndpoint() {
        return endpoint;
    }

    @JsonProperty("topicName")
    public String getQualifiedTopicName() {
        return TopicName.toQualifiedName(topicName);
    }

    @JsonIgnore
    public TopicName getTopicName() {
        return topicName;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public void setSerialSubscriptionPolicy(SubscriptionPolicy serialSubscriptionPolicy) {
        this.serialSubscriptionPolicy = serialSubscriptionPolicy;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    @JsonProperty("subscriptionPolicy")
    public Object getSubscriptionPolicy() {
        return isBatchSubscription() ? batchSubscriptionPolicy : serialSubscriptionPolicy;
    }

    public boolean isTrackingEnabled() {
        return trackingEnabled;
    }

    public String getSupportTeam() {
        return supportTeam;
    }

    public String getContact() {
        return contact;
    }

    public ContentType getContentType() {
        return contentType;
    }

    public DeliveryType getDeliveryType() {
        return deliveryType;
    }

    @JsonIgnore
    public boolean isBatchSubscription() {
        return this.deliveryType == DeliveryType.BATCH;
    }

    @JsonIgnore
    public BatchSubscriptionPolicy getBatchSubscriptionPolicy() {
        return batchSubscriptionPolicy;
    }

    @JsonIgnore
    public SubscriptionPolicy getSerialSubscriptionPolicy() {
        return serialSubscriptionPolicy;
    }

    @JsonIgnore
    public boolean isActive() {
        return state == State.ACTIVE || state == State.PENDING;
    }

    public Subscription anonymizePassword() {
        if (getEndpoint().containsCredentials()) {
            return Builder.subscription().withName(this.getName())
                    .withDescription(this.description)
                    .withTopicName(this.getTopicName())
                    .withEndpoint(this.getEndpoint().anonymizePassword())
                    .withState(this.getState())
                    .withSubscriptionPolicy(this.getSerialSubscriptionPolicy())
                    .withSubscriptionPolicy(this.getBatchSubscriptionPolicy())
                    .withTrackingEnabled(this.trackingEnabled)
                    .withSupportTeam(this.supportTeam)
                    .withContact(this.contact)
                    .build();
        }
        return this;
    }

    public static class Builder {
        private Subscription subscription;

        public Builder() {
            subscription = new Subscription();
        }

        public Builder withTopicName(String groupName, String topicName) {
            subscription.topicName = new TopicName(groupName, topicName);
            return this;
        }

        public Builder withTopicName(String qualifiedName) {
            subscription.topicName = TopicName.fromQualifiedName(qualifiedName);
            return this;
        }

        public Builder withTopicName(TopicName topicName) {
            subscription.topicName = topicName;
            return this;
        }

        public Builder withName(String name) {
            subscription.name = name;
            return this;
        }

        public Builder withState(State state) {
            subscription.state = state;
            return this;
        }

        public Builder withEndpoint(EndpointAddress endpointUri) {
            subscription.endpoint = endpointUri;
            return this;
        }

        public Builder withDescription(String description) {
            subscription.description = description;
            return this;
        }

        public Builder withSubscriptionPolicy(BatchSubscriptionPolicy subscriptionPolicy) {
            subscription.batchSubscriptionPolicy = subscriptionPolicy;
            subscription.deliveryType = DeliveryType.BATCH;
            return this;
        }

        public Builder withSubscriptionPolicy(SubscriptionPolicy subscriptionPolicy) {
            subscription.serialSubscriptionPolicy = subscriptionPolicy;
            subscription.deliveryType = DeliveryType.SERIAL;
            return this;
        }

        public Builder withTrackingEnabled(boolean trackingEnabled) {
            subscription.trackingEnabled = trackingEnabled;
            return this;
        }

        public Builder withSupportTeam(String supportTeam) {
            subscription.supportTeam = supportTeam;
            return this;
        }

        public Builder withContact(String contact) {
            subscription.contact = contact;
            return this;
        }

        public Builder applyPatch(Subscription subscription) {
            if (subscription != null) {
                clearPolicyIfDeliveryTypeChanged(subscription);
                this.subscription = Patch.apply(this.subscription, subscription);
            }
            return this;
        }

        private void clearPolicyIfDeliveryTypeChanged(Subscription subscription) {
            if (isDeliveryTypeChanged(subscription)) {
                this.subscription.serialSubscriptionPolicy = null;
                this.subscription.batchSubscriptionPolicy = null;
            }
        }

        private boolean isDeliveryTypeChanged(Subscription subscription) {
            return subscription.deliveryType != null && this.subscription.deliveryType != subscription.deliveryType;
        }

        public static Builder subscription() {
            return new Builder();
        }

        public Subscription build() {
            return subscription;
        }

        public Builder applyDefaults() {
            if (subscription.deliveryType == DeliveryType.BATCH) {
                subscription.batchSubscriptionPolicy = batchSubscriptionPolicy().applyDefaults().build();
            } else {
                subscription.serialSubscriptionPolicy = SubscriptionPolicy.Builder.subscriptionPolicy().applyDefaults().build();
            }
            return this;
        }

        public Builder withContentType(ContentType contentType) {
            subscription.contentType = contentType;
            return this;
        }
    }
}
