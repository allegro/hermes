package pl.allegro.tech.hermes.test.helper.builder;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import pl.allegro.tech.hermes.api.BatchSubscriptionPolicy;
import pl.allegro.tech.hermes.api.ContentType;
import pl.allegro.tech.hermes.api.DeliveryType;
import pl.allegro.tech.hermes.api.EndpointAddress;
import pl.allegro.tech.hermes.api.EndpointAddressResolverMetadata;
import pl.allegro.tech.hermes.api.Header;
import pl.allegro.tech.hermes.api.MessageFilterSpecification;
import pl.allegro.tech.hermes.api.MonitoringDetails;
import pl.allegro.tech.hermes.api.OwnerId;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.SubscriptionMode;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.api.SubscriptionOAuthPolicy;
import pl.allegro.tech.hermes.api.SubscriptionPolicy;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.api.TrackingMode;

public class SubscriptionBuilder {

  private static final AtomicLong sequence = new AtomicLong();

  private final TopicName topicName;

  private final String name;

  private Subscription.State state = Subscription.State.PENDING;

  private EndpointAddress endpoint = EndpointAddress.of("http://localhost:12345");

  private ContentType contentType = ContentType.JSON;

  private String description = "description";

  private SubscriptionPolicy serialSubscriptionPolicy =
      new SubscriptionPolicy(100, 10, 1000, 1000, false, 100, null, 0, 1, 600);

  private BatchSubscriptionPolicy batchSubscriptionPolicy;

  private boolean trackingEnabled = false;

  private TrackingMode trackingMode = TrackingMode.TRACKING_OFF;

  private boolean http2Enabled = false;

  private boolean profilingEnabled = false;

  private long profilingThresholdMs = 0;

  private OwnerId owner = new OwnerId("Plaintext", "some team");

  private MonitoringDetails monitoringDetails = MonitoringDetails.EMPTY;

  private DeliveryType deliveryType = DeliveryType.SERIAL;

  private final List<MessageFilterSpecification> filters = new ArrayList<>();

  private SubscriptionMode mode = SubscriptionMode.ANYCAST;

  private final List<Header> headers = new ArrayList<>();

  private EndpointAddressResolverMetadata metadata = EndpointAddressResolverMetadata.empty();
  private SubscriptionOAuthPolicy oAuthPolicy;

  private boolean attachingIdentityHeadersEnabled = false;

  private boolean autoDeleteWithTopicEnabled = false;

  private SubscriptionBuilder(
      TopicName topicName, String subscriptionName, EndpointAddress endpoint) {
    this.topicName = topicName;
    this.name = subscriptionName;
    this.endpoint = endpoint;
  }

  private SubscriptionBuilder(TopicName topicName, String subscriptionName) {
    this.topicName = topicName;
    this.name = subscriptionName;
  }

  public static SubscriptionBuilder subscriptionWithRandomName(
      TopicName topicName, String endpoint) {
    return new SubscriptionBuilder(
        topicName, "subscription" + sequence.incrementAndGet(), EndpointAddress.of(endpoint));
  }

  public static SubscriptionBuilder subscriptionWithRandomName(TopicName topicName) {
    return new SubscriptionBuilder(topicName, UUID.randomUUID().toString());
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

  public static SubscriptionBuilder subscription(
      TopicName topicName, String subscriptionName, EndpointAddress endpoint) {
    return new SubscriptionBuilder(topicName, subscriptionName, endpoint);
  }

  public static SubscriptionBuilder subscription(
      String topicQualifiedName, String subscriptionName) {
    return new SubscriptionBuilder(
        TopicName.fromQualifiedName(topicQualifiedName), subscriptionName);
  }

  public static SubscriptionBuilder subscription(
      String topicQualifiedName, String subscriptionName, String endpoint) {
    return subscription(
        TopicName.fromQualifiedName(topicQualifiedName),
        subscriptionName,
        EndpointAddress.of(endpoint));
  }

  public static SubscriptionBuilder subscription(
      String topicQualifiedName, String subscriptionName, URI endpoint) {
    return subscription(
        TopicName.fromQualifiedName(topicQualifiedName),
        subscriptionName,
        EndpointAddress.of(endpoint));
  }

  public static SubscriptionBuilder subscription(
      String topicQualifiedName, String subscriptionName, EndpointAddress endpoint) {
    return subscription(
        TopicName.fromQualifiedName(topicQualifiedName), subscriptionName, endpoint);
  }

  public Subscription build() {
    if (deliveryType == DeliveryType.SERIAL) {
      return Subscription.createSerialSubscription(
          topicName,
          name,
          endpoint,
          state,
          description,
          serialSubscriptionPolicy,
          trackingEnabled,
          trackingMode,
          owner,
          monitoringDetails,
          contentType,
          filters,
          mode,
          headers,
          metadata,
          oAuthPolicy,
          http2Enabled,
          profilingEnabled,
          profilingThresholdMs,
          attachingIdentityHeadersEnabled,
          autoDeleteWithTopicEnabled);
    } else {
      return Subscription.createBatchSubscription(
          topicName,
          name,
          endpoint,
          state,
          description,
          batchSubscriptionPolicy,
          trackingEnabled,
          trackingMode,
          owner,
          monitoringDetails,
          contentType,
          filters,
          headers,
          metadata,
          oAuthPolicy,
          http2Enabled,
          attachingIdentityHeadersEnabled,
          autoDeleteWithTopicEnabled);
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

  public SubscriptionBuilder withEndpoint(URI endpoint) {
    this.endpoint = EndpointAddress.of(endpoint.toString());
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
    this.serialSubscriptionPolicy =
        SubscriptionPolicy.Builder.subscriptionPolicy()
            .withRate(policy.getRate())
            .withMessageTtl(policy.getMessageTtl())
            .withMessageBackoff(policy.getMessageBackoff())
            .withRequestTimeout(timeout)
            .build();
    return this;
  }

  public SubscriptionBuilder withTrackingMode(TrackingMode trackingMode) {
    this.trackingMode = trackingMode;
    this.trackingEnabled = trackingMode != TrackingMode.TRACKING_OFF;
    return this;
  }

  public SubscriptionBuilder withHttp2Enabled(boolean http2Enabled) {
    this.http2Enabled = http2Enabled;
    return this;
  }

  public SubscriptionBuilder withProfilingEnabled(boolean profilingEnabled) {
    this.profilingEnabled = profilingEnabled;
    return this;
  }

  public SubscriptionBuilder withProfilingThresholdMs(long profilingThresholdMs) {
    this.profilingThresholdMs = profilingThresholdMs;
    return this;
  }

  public SubscriptionBuilder withOwner(OwnerId owner) {
    this.owner = owner;
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

  public SubscriptionBuilder withMode(SubscriptionMode mode) {
    this.mode = mode;
    return this;
  }

  public SubscriptionBuilder withHeader(String name, String value) {
    this.headers.add(new Header(name, value));
    return this;
  }

  public SubscriptionBuilder withEndpointAddressResolverMetadata(
      EndpointAddressResolverMetadata metadata) {
    this.metadata = metadata;
    return this;
  }

  public SubscriptionBuilder withOAuthPolicy(SubscriptionOAuthPolicy oAuthPolicy) {
    this.oAuthPolicy = oAuthPolicy;
    return this;
  }

  public SubscriptionBuilder withAttachingIdentityHeadersEnabled(
      boolean attachingIdentityHeadersEnabled) {
    this.attachingIdentityHeadersEnabled = attachingIdentityHeadersEnabled;
    return this;
  }

  public SubscriptionBuilder withAutoDeleteWithTopicEnabled(boolean autoDeleteWithTopicEnabled) {
    this.autoDeleteWithTopicEnabled = autoDeleteWithTopicEnabled;
    return this;
  }
}
