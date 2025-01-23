package pl.allegro.tech.hermes.integrationtests.subscriber;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.NoCredentialsProvider;
import com.google.api.gax.grpc.GrpcTransportChannel;
import com.google.api.gax.rpc.FixedTransportChannelProvider;
import com.google.api.gax.rpc.TransportChannelProvider;
import com.google.cloud.pubsub.v1.SubscriptionAdminClient;
import com.google.cloud.pubsub.v1.SubscriptionAdminSettings;
import com.google.cloud.pubsub.v1.TopicAdminClient;
import com.google.cloud.pubsub.v1.TopicAdminSettings;
import com.google.cloud.pubsub.v1.stub.GrpcSubscriberStub;
import com.google.cloud.pubsub.v1.stub.SubscriberStub;
import com.google.cloud.pubsub.v1.stub.SubscriberStubSettings;
import com.google.pubsub.v1.ProjectSubscriptionName;
import com.google.pubsub.v1.PullRequest;
import com.google.pubsub.v1.PullResponse;
import com.google.pubsub.v1.PushConfig;
import com.google.pubsub.v1.ReceivedMessage;
import com.google.pubsub.v1.TopicName;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class TestGooglePubSubSubscriber {

  private static final String GOOGLE_PUBSUB_PROJECT_ID = "test-project";
  private static final AtomicInteger sequence = new AtomicInteger();

  private final CredentialsProvider credentialsProvider;
  private final TransportChannelProvider transportChannelProvider;
  private final ManagedChannel channel;
  private final String endpoint;
  private final String subscription;
  private final List<ReceivedMessage> receivedMessages = new ArrayList<>();

  public TestGooglePubSubSubscriber(String emulatorEndpoint) {
    this.credentialsProvider = NoCredentialsProvider.create();
    this.channel = ManagedChannelBuilder.forTarget(emulatorEndpoint).usePlaintext().build();
    this.transportChannelProvider =
        FixedTransportChannelProvider.create(GrpcTransportChannel.create(channel));
    String topicId = "test-topic" + sequence.incrementAndGet();
    String topic = TopicName.format(GOOGLE_PUBSUB_PROJECT_ID, topicId);
    subscription =
        ProjectSubscriptionName.format(
            GOOGLE_PUBSUB_PROJECT_ID, "test-subscription" + sequence.incrementAndGet());
    try {
      createTopic(topic);
      createSubscription(subscription, topic);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    endpoint =
        "googlepubsub://pubsub.googleapis.com:443/projects/%s/topics/%s"
            .formatted(GOOGLE_PUBSUB_PROJECT_ID, topicId);
  }

  private void createTopic(String topicName) throws IOException {
    TopicAdminSettings topicAdminSettings =
        TopicAdminSettings.newBuilder()
            .setTransportChannelProvider(transportChannelProvider)
            .setCredentialsProvider(credentialsProvider)
            .build();
    try (TopicAdminClient topicAdminClient = TopicAdminClient.create(topicAdminSettings)) {
      topicAdminClient.createTopic(topicName);
    }
  }

  private void createSubscription(String subscriptionName, String topicName) throws IOException {
    SubscriptionAdminSettings subscriptionAdminSettings =
        SubscriptionAdminSettings.newBuilder()
            .setTransportChannelProvider(transportChannelProvider)
            .setCredentialsProvider(credentialsProvider)
            .build();
    SubscriptionAdminClient subscriptionAdminClient =
        SubscriptionAdminClient.create(subscriptionAdminSettings);
    subscriptionAdminClient.createSubscription(
        subscriptionName, topicName, PushConfig.getDefaultInstance(), 10);
  }

  public String getEndpoint() {
    return endpoint;
  }

  public void waitUntilAnyMessageReceived() throws IOException {
    SubscriberStubSettings subscriberStubSettings =
        SubscriberStubSettings.newBuilder()
            .setTransportChannelProvider(transportChannelProvider)
            .setCredentialsProvider(credentialsProvider)
            .build();
    try (SubscriberStub subscriber = GrpcSubscriberStub.create(subscriberStubSettings)) {
      PullRequest pullRequest =
          PullRequest.newBuilder().setMaxMessages(1).setSubscription(subscription).build();
      PullResponse pullResponse = subscriber.pullCallable().call(pullRequest);
      assertThat(pullResponse.getReceivedMessagesList()).isNotEmpty();
      receivedMessages.addAll(pullResponse.getReceivedMessagesList());
    }
  }

  public List<ReceivedMessage> getAllReceivedMessages() {
    return receivedMessages;
  }

  public void stop() {
    channel.shutdown();
  }
}
