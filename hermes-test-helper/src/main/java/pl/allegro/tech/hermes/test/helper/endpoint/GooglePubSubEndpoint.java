package pl.allegro.tech.hermes.test.helper.endpoint;

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
import com.google.pubsub.v1.PubsubMessage;
import com.google.pubsub.v1.PullRequest;
import com.google.pubsub.v1.PullResponse;
import com.google.pubsub.v1.PushConfig;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import pl.allegro.tech.hermes.test.helper.containers.GooglePubSubContainer;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class GooglePubSubEndpoint {

    private final CredentialsProvider credentialsProvider;
    private final TransportChannelProvider transportChannelProvider;
    private final ManagedChannel channel;

    public GooglePubSubEndpoint(GooglePubSubContainer container) {
        this.credentialsProvider = NoCredentialsProvider.create();
        this.channel = ManagedChannelBuilder.forTarget(container.getEmulatorEndpoint()).usePlaintext().build();
        this.transportChannelProvider = FixedTransportChannelProvider.create(GrpcTransportChannel.create(channel));
    }

    public void createTopic(String topicName) throws IOException {
        TopicAdminSettings topicAdminSettings = TopicAdminSettings.newBuilder()
                .setTransportChannelProvider(transportChannelProvider)
                .setCredentialsProvider(credentialsProvider)
                .build();
        try (TopicAdminClient topicAdminClient = TopicAdminClient.create(topicAdminSettings)) {
            topicAdminClient.createTopic(topicName);
        }
    }

    public void createSubscription(String subscriptionName, String topicName) throws IOException {
        SubscriptionAdminSettings subscriptionAdminSettings = SubscriptionAdminSettings.newBuilder()
                .setTransportChannelProvider(transportChannelProvider)
                .setCredentialsProvider(credentialsProvider)
                .build();
        SubscriptionAdminClient subscriptionAdminClient = SubscriptionAdminClient.create(subscriptionAdminSettings);
        subscriptionAdminClient.createSubscription(subscriptionName, topicName, PushConfig.getDefaultInstance(), 10);
    }

    public PubsubMessage messageReceived(String subscription) throws IOException {
        SubscriberStubSettings subscriberStubSettings =
                SubscriberStubSettings.newBuilder()
                        .setTransportChannelProvider(transportChannelProvider)
                        .setCredentialsProvider(credentialsProvider)
                        .build();
        try (SubscriberStub subscriber = GrpcSubscriberStub.create(subscriberStubSettings)) {
            PullRequest pullRequest = PullRequest.newBuilder()
                    .setMaxMessages(1)
                    .setSubscription(subscription)
                    .build();
            PullResponse pullResponse = subscriber.pullCallable().call(pullRequest);

            assertThat(pullResponse.getReceivedMessagesList()).hasSize(1);
            return pullResponse.getReceivedMessages(0).getMessage();
        }
    }

    public void stop() {
        channel.shutdown();
    }
}
