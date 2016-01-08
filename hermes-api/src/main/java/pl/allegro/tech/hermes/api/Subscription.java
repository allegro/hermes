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
import java.util.Objects;

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
    private SubscriptionPolicy subscriptionPolicy;

    private boolean trackingEnabled;

    @NotNull
    private String supportTeam;

    private String contact;


    public enum State {
        PENDING, ACTIVE, SUSPENDED
    }

    private Subscription() {
    }

    public Subscription(TopicName topicName, String name, EndpointAddress endpoint, String description,
                        SubscriptionPolicy subscriptionPolicy, boolean trackingEnabled, String supportTeam, String contact,
                        ContentType contentType) {
        this.topicName = topicName;
        this.name = name;
        this.description = description;
        this.subscriptionPolicy = subscriptionPolicy;
        this.endpoint = endpoint;
        this.trackingEnabled = trackingEnabled;
        this.supportTeam = supportTeam;
        this.contact = contact;
        this.contentType = contentType == null ? ContentType.JSON : contentType;
    }

    @JsonCreator
    public Subscription(@JsonProperty("topicName") String topicName,
                        @JsonProperty("name") String name,
                        @JsonProperty("endpoint") EndpointAddress endpoint,
                        @JsonProperty("description") String description,
                        @JsonProperty("subscriptionPolicy") SubscriptionPolicy subscriptionPolicy,
                        @JsonProperty("trackingEnabled") boolean trackingEnabled,
                        @JsonProperty("supportTeam") String supportTeam,
                        @JsonProperty("contact") String contact,
                        @JsonProperty("contentType") ContentType contentType) {
        this(TopicName.fromQualifiedName(topicName), name, endpoint, description, subscriptionPolicy,
                trackingEnabled, supportTeam, contact, contentType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(endpoint, topicName, name, description, subscriptionPolicy, contentType);
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
                && Objects.equals(this.subscriptionPolicy, other.subscriptionPolicy)
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

    public void setSubscriptionPolicy(SubscriptionPolicy subscriptionPolicy) {
        this.subscriptionPolicy = subscriptionPolicy;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public SubscriptionPolicy getSubscriptionPolicy() {
        return subscriptionPolicy;
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
                    .withSubscriptionPolicy(this.getSubscriptionPolicy())
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

        public Builder withSubscriptionPolicy(SubscriptionPolicy subscriptionPolicy) {
            subscription.subscriptionPolicy = subscriptionPolicy;
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
                this.subscription = Patch.apply(this.subscription, subscription);
            }
            return this;
        }

        public static Builder subscription() {
            return new Builder();
        }

        public Subscription build() {
            return subscription;
        }

        public Builder applyDefaults() {
            subscription.subscriptionPolicy = SubscriptionPolicy.Builder.subscriptionPolicy().applyDefaults().build();
            return this;
        }

        public Builder withContentType(ContentType contentType) {
            subscription.contentType = contentType;
            return this;
        }
    }
}
