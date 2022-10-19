package pl.allegro.tech.hermes.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import pl.allegro.tech.hermes.api.constraints.ValidContentType;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import static pl.allegro.tech.hermes.api.constraints.Names.ALLOWED_NAME_REGEX;

@ValidContentType(message = "AVRO content type is not supported in BATCH delivery mode")
@JsonIgnoreProperties(value = {"createdAt", "modifiedAt"}, allowGetters = true)
public class Subscription implements Anonymizable {

    @NotNull
    private final MonitoringDetails monitoringDetails;
    private final SubscriptionName subscriptionName;
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
    /**
     * Use trackingMode field instead.
     */
    @Deprecated
    private boolean trackingEnabled = false;
    private TrackingMode trackingMode = TrackingMode.TRACKING_OFF;
    private boolean http2Enabled = false;
    @Valid
    @NotNull
    private OwnerId owner;
    @NotNull
    private DeliveryType deliveryType = DeliveryType.SERIAL;
    @NotNull
    private SubscriptionMode mode = SubscriptionMode.ANYCAST;
    private List<MessageFilterSpecification> filters = new ArrayList<>();

    private List<Header> headers;

    private EndpointAddressResolverMetadata endpointAddressResolverMetadata;

    @Valid
    private SubscriptionOAuthPolicy oAuthPolicy;

    private boolean subscriptionIdentityHeadersEnabled;

    private boolean autoDeleteWithTopicEnabled;

    private Instant createdAt;

    private Instant modifiedAt;

    private Subscription(TopicName topicName,
                         String name,
                         EndpointAddress endpoint,
                         State state,
                         String description,
                         Object subscriptionPolicy,
                         boolean trackingEnabled,
                         TrackingMode trackingMode,
                         OwnerId owner,
                         MonitoringDetails monitoringDetails,
                         ContentType contentType,
                         DeliveryType deliveryType,
                         List<MessageFilterSpecification> filters,
                         SubscriptionMode mode,
                         List<Header> headers,
                         EndpointAddressResolverMetadata endpointAddressResolverMetadata,
                         SubscriptionOAuthPolicy oAuthPolicy,
                         boolean http2Enabled,
                         boolean subscriptionIdentityHeadersEnabled,
                         boolean autoDeleteWithTopicEnabled) {
        this.topicName = topicName;
        this.name = name;
        this.endpoint = endpoint;
        this.state = state != null ? state : State.PENDING;
        this.description = description;
        this.trackingEnabled = trackingEnabled;
        this.trackingMode = trackingMode;
        this.owner = owner;
        this.monitoringDetails = monitoringDetails == null ? MonitoringDetails.EMPTY : monitoringDetails;
        this.contentType = contentType == null ? ContentType.JSON : contentType;
        this.deliveryType = deliveryType;
        this.batchSubscriptionPolicy = this.deliveryType == DeliveryType.BATCH ? (BatchSubscriptionPolicy) subscriptionPolicy : null;
        this.serialSubscriptionPolicy = this.deliveryType == DeliveryType.SERIAL ? (SubscriptionPolicy) subscriptionPolicy : null;
        this.filters = filters;
        this.mode = mode;
        this.http2Enabled = http2Enabled;
        this.subscriptionName = new SubscriptionName(name, topicName);
        this.headers = headers;
        this.endpointAddressResolverMetadata = endpointAddressResolverMetadata;
        this.oAuthPolicy = oAuthPolicy;
        this.subscriptionIdentityHeadersEnabled = subscriptionIdentityHeadersEnabled;
        this.autoDeleteWithTopicEnabled = autoDeleteWithTopicEnabled;
    }

    public static Subscription createSerialSubscription(TopicName topicName,
                                                        String name,
                                                        EndpointAddress endpoint,
                                                        State state,
                                                        String description,
                                                        SubscriptionPolicy subscriptionPolicy,
                                                        boolean trackingEnabled,
                                                        TrackingMode trackingMode,
                                                        OwnerId owner,
                                                        MonitoringDetails monitoringDetails,
                                                        ContentType contentType,
                                                        List<MessageFilterSpecification> filters,
                                                        SubscriptionMode mode,
                                                        List<Header> headers,
                                                        EndpointAddressResolverMetadata endpointAddressResolverMetadata,
                                                        SubscriptionOAuthPolicy oAuthPolicy,
                                                        boolean http2Enabled,
                                                        boolean subscriptionIdentityHeadersEnabled,
                                                        boolean autoDeleteWithTopicEnabled) {
        return new Subscription(topicName, name, endpoint, state, description, subscriptionPolicy, trackingEnabled, trackingMode,
                owner, monitoringDetails, contentType, DeliveryType.SERIAL, filters, mode, headers,
                endpointAddressResolverMetadata, oAuthPolicy, http2Enabled, subscriptionIdentityHeadersEnabled, autoDeleteWithTopicEnabled);
    }

    public static Subscription createBatchSubscription(TopicName topicName,
                                                       String name,
                                                       EndpointAddress endpoint,
                                                       State state,
                                                       String description,
                                                       BatchSubscriptionPolicy subscriptionPolicy,
                                                       boolean trackingEnabled,
                                                       TrackingMode trackingMode,
                                                       OwnerId owner,
                                                       MonitoringDetails monitoringDetails,
                                                       ContentType contentType,
                                                       List<MessageFilterSpecification> filters,
                                                       List<Header> headers,
                                                       EndpointAddressResolverMetadata endpointAddressResolverMetadata,
                                                       SubscriptionOAuthPolicy oAuthPolicy,
                                                       boolean http2Enabled,
                                                       boolean subscriptionIdentityHeadersEnabled,
                                                       boolean autoDeleteWithTopicEnabled) {
        return new Subscription(topicName, name, endpoint, state, description, subscriptionPolicy, trackingEnabled, trackingMode,
                owner, monitoringDetails, contentType, DeliveryType.BATCH, filters, SubscriptionMode.ANYCAST, headers,
                endpointAddressResolverMetadata, oAuthPolicy, http2Enabled, subscriptionIdentityHeadersEnabled, autoDeleteWithTopicEnabled);
    }

    @JsonCreator
    public static Subscription create(
            @JsonProperty("topicName") String topicName,
            @JsonProperty("name") String name,
            @JsonProperty("endpoint") EndpointAddress endpoint,
            @JsonProperty("state") State state,
            @JsonProperty("description") String description,
            @JsonProperty("subscriptionPolicy") Map<String, Object> subscriptionPolicy,
            @JsonProperty("trackingEnabled") boolean trackingEnabled,
            @JsonProperty("trackingMode") String trackingMode,
            @JsonProperty("owner") OwnerId owner,
            @JsonProperty("monitoringDetails") MonitoringDetails monitoringDetails,
            @JsonProperty("contentType") ContentType contentType,
            @JsonProperty("deliveryType") DeliveryType deliveryType,
            @JsonProperty("filters") List<MessageFilterSpecification> filters,
            @JsonProperty("mode") SubscriptionMode mode,
            @JsonProperty("headers") List<Header> headers,
            @JsonProperty("endpointAddressResolverMetadata") EndpointAddressResolverMetadata endpointAddressResolverMetadata,
            @JsonProperty("oAuthPolicy") SubscriptionOAuthPolicy oAuthPolicy,
            @JsonProperty("http2Enabled") boolean http2Enabled,
            @JsonProperty("subscriptionIdentityHeadersEnabled") boolean subscriptionIdentityHeadersEnabled,
            @JsonProperty("autoDeleteWithTopicEnabled") boolean autoDeleteWithTopicEnabled) {

        DeliveryType validDeliveryType = deliveryType == null ? DeliveryType.SERIAL : deliveryType;
        SubscriptionMode subscriptionMode = mode == null ? SubscriptionMode.ANYCAST : mode;
        Map<String, Object> validSubscriptionPolicy = subscriptionPolicy == null ? new HashMap<>() : subscriptionPolicy;

        TrackingMode validTrackingMode = TrackingMode.fromString(trackingMode)
                .orElse(trackingEnabled ? TrackingMode.TRACK_ALL : TrackingMode.TRACKING_OFF);
        boolean validTrackingEnabled = validTrackingMode != TrackingMode.TRACKING_OFF;

        return new Subscription(
                TopicName.fromQualifiedName(topicName),
                name,
                endpoint,
                state,
                description,
                validDeliveryType == DeliveryType.SERIAL
                        ? SubscriptionPolicy.create(validSubscriptionPolicy)
                        : BatchSubscriptionPolicy.create(validSubscriptionPolicy),
                validTrackingEnabled,
                validTrackingMode,
                owner,
                monitoringDetails,
                contentType,
                validDeliveryType,
                filters == null ? Collections.emptyList() : filters,
                subscriptionMode,
                headers == null ? Collections.emptyList() : headers,
                endpointAddressResolverMetadata == null ? EndpointAddressResolverMetadata.empty() : endpointAddressResolverMetadata,
                oAuthPolicy,
                http2Enabled,
                subscriptionIdentityHeadersEnabled,
                autoDeleteWithTopicEnabled
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(endpoint, topicName, name, description, serialSubscriptionPolicy, batchSubscriptionPolicy,
                trackingEnabled, trackingMode, owner, monitoringDetails, contentType, filters, mode, headers,
                endpointAddressResolverMetadata, oAuthPolicy, http2Enabled, subscriptionIdentityHeadersEnabled);
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
                && Objects.equals(this.trackingMode, other.trackingMode)
                && Objects.equals(this.owner, other.owner)
                && Objects.equals(this.monitoringDetails, other.monitoringDetails)
                && Objects.equals(this.contentType, other.contentType)
                && Objects.equals(this.filters, other.filters)
                && Objects.equals(this.mode, other.mode)
                && Objects.equals(this.headers, other.headers)
                && Objects.equals(this.endpointAddressResolverMetadata, other.endpointAddressResolverMetadata)
                && Objects.equals(this.http2Enabled, other.http2Enabled)
                && Objects.equals(this.oAuthPolicy, other.oAuthPolicy)
                && Objects.equals(this.subscriptionIdentityHeadersEnabled, other.subscriptionIdentityHeadersEnabled)
                && Objects.equals(this.autoDeleteWithTopicEnabled, other.autoDeleteWithTopicEnabled);
    }

    @JsonIgnore
    public SubscriptionName getQualifiedName() {
        return subscriptionName;
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
        return trackingMode != TrackingMode.TRACKING_OFF;
    }

    @JsonProperty("trackingMode")
    public String getTrackingModeString() {
        return trackingMode.getValue();
    }

    @JsonIgnore
    public TrackingMode getTrackingMode() {
        return trackingMode;
    }

    public OwnerId getOwner() {
        return owner;
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

    public List<MessageFilterSpecification> getFilters() {
        return Collections.unmodifiableList(filters);
    }

    public List<Header> getHeaders() {
        return Collections.unmodifiableList(headers);
    }

    public EndpointAddressResolverMetadata getEndpointAddressResolverMetadata() {
        return endpointAddressResolverMetadata;
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

    public void setSerialSubscriptionPolicy(SubscriptionPolicy serialSubscriptionPolicy) {
        this.serialSubscriptionPolicy = serialSubscriptionPolicy;
    }

    @JsonIgnore
    public boolean isActive() {
        return state == State.ACTIVE || state == State.PENDING;
    }

    public SubscriptionMode getMode() {
        return mode;
    }

    @JsonProperty("oAuthPolicy")
    public SubscriptionOAuthPolicy getOAuthPolicy() {
        return oAuthPolicy;
    }

    @JsonIgnore
    public boolean hasOAuthPolicy() {
        return oAuthPolicy != null;
    }

    @JsonIgnore
    public boolean isSeverityNotImportant() {
        return getMonitoringDetails().getSeverity() == MonitoringDetails.Severity.NON_IMPORTANT;
    }

    public boolean isHttp2Enabled() {
        return http2Enabled;
    }

    public boolean isSubscriptionIdentityHeadersEnabled() {
        return subscriptionIdentityHeadersEnabled;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Long createdAt) {
        this.createdAt = Instant.ofEpochMilli(createdAt);
    }

    public Instant getModifiedAt() {
        return modifiedAt;
    }

    public void setModifiedAt(Long modifiedAt) {
        this.modifiedAt = Instant.ofEpochMilli(modifiedAt);
    }

    public boolean isAutoDeleteWithTopicEnabled() {
        return autoDeleteWithTopicEnabled;
    }

    @Override
    public Subscription anonymize() {
        if (getEndpoint() != null && getEndpoint().containsCredentials() || hasOAuthPolicy()) {
            return new Subscription(
                    topicName,
                    name,
                    endpoint.anonymize(),
                    state,
                    description,
                    deliveryType == DeliveryType.BATCH ? batchSubscriptionPolicy : serialSubscriptionPolicy,
                    trackingEnabled,
                    trackingMode,
                    owner,
                    monitoringDetails,
                    contentType,
                    deliveryType,
                    filters,
                    mode,
                    headers,
                    endpointAddressResolverMetadata,
                    oAuthPolicy != null ? oAuthPolicy.anonymize() : null,
                    http2Enabled,
                    subscriptionIdentityHeadersEnabled,
                    autoDeleteWithTopicEnabled
            );
        }
        return this;
    }

    @Override
    public String toString() {
        return "Subscription(" + getQualifiedName() + ")";
    }

    public enum State {
        PENDING, ACTIVE, SUSPENDED
    }
}
