package pl.allegro.tech.hermes.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Joiner;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static pl.allegro.tech.hermes.api.constraints.Names.ALLOWED_NAME_REGEX;
import static pl.allegro.tech.hermes.api.helpers.Replacer.replaceInAll;

public class Subscription {

    @Valid
    @NotNull
    private TopicName topicName;

    @NotEmpty
    @Pattern(regexp = ALLOWED_NAME_REGEX)
    private String name;

    private State state = State.PENDING;

    @NotNull
    @Valid
    private EndpointAddress endpoint;

    @NotNull
    private ContentType contentType = ContentType.JSON;

    @NotNull
    private String description;

    @Valid
    private SubscriptionPolicy serialSubscriptionPolicy;

    @Valid
    private BatchSubscriptionPolicy batchSubscriptionPolicy;

    private boolean trackingEnabled = false;

    @NotNull
    private String supportTeam;

    private String contact;

    @NotNull
    private final MonitoringDetails monitoringDetails;

    @NotNull
    private DeliveryType deliveryType = DeliveryType.SERIAL;

    private List<MessageFilterSpecification> filters = new ArrayList<>();

    public List<MessageFilterSpecification> getFilters() {
        return filters;
    }

    public enum State {
        PENDING, ACTIVE, SUSPENDED
    }

    private Subscription(TopicName topicName,
                         String name,
                         EndpointAddress endpoint,
                         State state,
                         String description,
                         Object subscriptionPolicy,
                         boolean trackingEnabled,
                         String supportTeam,
                         String contact,
                         MonitoringDetails monitoringDetails,
                         ContentType contentType,
                         DeliveryType deliveryType,
                         List<MessageFilterSpecification> filters) {
        this.topicName = topicName;
        this.name = name;
        this.endpoint = endpoint;
        this.filters = filters;
        this.state = state != null ? state : State.PENDING;
        this.description = description;
        this.trackingEnabled = trackingEnabled;
        this.supportTeam = supportTeam;
        this.contact = contact;
        this.monitoringDetails = monitoringDetails == null ? MonitoringDetails.EMPTY : monitoringDetails;
        this.contentType = contentType == null ? ContentType.JSON : contentType;
        this.deliveryType = deliveryType;
        this.batchSubscriptionPolicy = this.deliveryType == DeliveryType.BATCH ? (BatchSubscriptionPolicy) subscriptionPolicy : null;
        this.serialSubscriptionPolicy = this.deliveryType == DeliveryType.SERIAL ? (SubscriptionPolicy) subscriptionPolicy : null;
    }

    public static Subscription createSerialSubscription(TopicName topicName,
                                                        String name,
                                                        EndpointAddress endpoint,
                                                        State state,
                                                        String description,
                                                        SubscriptionPolicy subscriptionPolicy,
                                                        boolean trackingEnabled,
                                                        String supportTeam,
                                                        String contact,
                                                        MonitoringDetails monitoringDetails,
                                                        ContentType contentType,
                                                        List<MessageFilterSpecification> filters) {
        return new Subscription(topicName, name, endpoint, state, description, subscriptionPolicy, trackingEnabled, supportTeam,
                contact, monitoringDetails, contentType, DeliveryType.SERIAL, filters);
    }

    public static Subscription createBatchSubscription(TopicName topicName,
                                                       String name,
                                                       EndpointAddress endpoint,
                                                       State state,
                                                       String description,
                                                       BatchSubscriptionPolicy subscriptionPolicy,
                                                       boolean trackingEnabled,
                                                       String supportTeam,
                                                       String contact,
                                                       MonitoringDetails monitoringDetails,
                                                       ContentType contentType,
                                                       List<MessageFilterSpecification> filters) {
        return new Subscription(topicName, name, endpoint, state, description, subscriptionPolicy, trackingEnabled, supportTeam,
                contact, monitoringDetails, contentType, DeliveryType.BATCH, filters);
    }

    @JsonCreator
    public static Subscription create(@JsonProperty("topicName") String topicName,
                                      @JsonProperty("name") String name,
                                      @JsonProperty("endpoint") EndpointAddress endpoint,
                                      @JsonProperty("state") State state,
                                      @JsonProperty("description") String description,
                                      @JsonProperty("subscriptionPolicy") Map<String, Object> subscriptionPolicy,
                                      @JsonProperty("trackingEnabled") boolean trackingEnabled,
                                      @JsonProperty("supportTeam") String supportTeam,
                                      @JsonProperty("contact") String contact,
                                      @JsonProperty("monitoringDetails") MonitoringDetails monitoringDetails,
                                      @JsonProperty("contentType") ContentType contentType,
                                      @JsonProperty("deliveryType") DeliveryType deliveryType,
                                      @JsonProperty("filters") List<MessageFilterSpecification> filters) {
        DeliveryType validDeliveryType = deliveryType == null ? DeliveryType.SERIAL : deliveryType;
        Map<String, Object> validSubscriptionPolicy = subscriptionPolicy == null ? new HashMap<>() : subscriptionPolicy;

        return new Subscription(
                TopicName.fromQualifiedName(topicName),
                name,
                endpoint,
                state,
                description,
                validDeliveryType == DeliveryType.SERIAL ?
                        SubscriptionPolicy.create(validSubscriptionPolicy) : BatchSubscriptionPolicy.create(validSubscriptionPolicy),
                trackingEnabled,
                supportTeam,
                contact,
                monitoringDetails,
                contentType,
                validDeliveryType,
                filters == null? new ArrayList<>() : filters
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(endpoint, topicName, name, description, serialSubscriptionPolicy, batchSubscriptionPolicy, trackingEnabled, supportTeam, contact, monitoringDetails, contentType, filters);
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
                && Objects.equals(this.monitoringDetails, other.monitoringDetails)
                && Objects.equals(this.contentType, other.contentType)
                && Objects.equals(this.filters, other.filters);
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

    public MonitoringDetails getMonitoringDetails() {
        return monitoringDetails;
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
            return new Subscription(
                    topicName,
                    name,
                    endpoint.anonymizePassword(),
                    state,
                    description,
                    deliveryType == DeliveryType.BATCH ? batchSubscriptionPolicy : serialSubscriptionPolicy,
                    trackingEnabled,
                    supportTeam,
                    contact,
                    monitoringDetails,
                    contentType,
                    deliveryType,
                    filters
            );
        }
        return this;
    }
}
