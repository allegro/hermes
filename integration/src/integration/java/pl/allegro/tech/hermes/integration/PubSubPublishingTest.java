package pl.allegro.tech.hermes.integration;

import com.google.api.client.util.Lists;
import com.google.api.gax.batching.BatchingSettings;
import com.google.api.gax.core.FixedExecutorProvider;
import com.google.api.gax.core.NoCredentialsProvider;
import com.google.api.gax.grpc.GrpcTransportChannel;
import com.google.api.gax.retrying.RetrySettings;
import com.google.api.gax.rpc.FixedTransportChannelProvider;
import com.google.api.gax.rpc.TransportChannelProvider;
import com.google.cloud.pubsub.v1.SubscriptionAdminClient;
import com.google.cloud.pubsub.v1.SubscriptionAdminSettings;
import com.google.cloud.pubsub.v1.TopicAdminClient;
import com.google.cloud.pubsub.v1.TopicAdminSettings;
import com.google.cloud.pubsub.v1.stub.GrpcSubscriberStub;
import com.google.cloud.pubsub.v1.stub.SubscriberStub;
import com.google.cloud.pubsub.v1.stub.SubscriberStubSettings;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.pubsub.v1.ProjectSubscriptionName;
import com.google.pubsub.v1.PullRequest;
import com.google.pubsub.v1.PullResponse;
import com.google.pubsub.v1.PushConfig;
import com.google.pubsub.v1.TopicName;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.testcontainers.containers.PubSubEmulatorContainer;
import org.testcontainers.utility.DockerImageName;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.threeten.bp.Duration;
import pl.allegro.tech.hermes.api.ContentType;
import pl.allegro.tech.hermes.common.kafka.KafkaTopicName;
import pl.allegro.tech.hermes.common.kafka.offset.PartitionOffset;
import pl.allegro.tech.hermes.consumers.consumer.Message;
import pl.allegro.tech.hermes.consumers.consumer.sender.googlepubsub.GooglePubSubClientsPool;
import pl.allegro.tech.hermes.consumers.consumer.sender.googlepubsub.GooglePubSubMessageSender;
import pl.allegro.tech.hermes.consumers.consumer.sender.googlepubsub.GooglePubSubMessages;
import pl.allegro.tech.hermes.consumers.consumer.sender.googlepubsub.GooglePubSubMetadataAppender;
import pl.allegro.tech.hermes.consumers.consumer.sender.googlepubsub.GooglePubSubSenderTarget;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

public class PubSubPublishingTest/* extends IntegrationTest */ {

    private static final String projectId = "test-project";
    private static final String topicId = "hermes-in-pubsub";
    private static final String subscriptionId = "hermes-in-pubsub-sub";

    public static PubSubEmulatorContainer pubSubEmulator;

    private static GooglePubSubSenderTarget senderTarget;

    private static GooglePubSubMessageSender messageSender;

    private static ManagedChannel channel;
    private static String hostPort;
    private static TransportChannelProvider channelProvider;
    private static final NoCredentialsProvider credentialsProvider = NoCredentialsProvider.create();


    @BeforeClass
    public static void startContainer() throws IOException, InterruptedException {

        pubSubEmulator = new PubSubEmulatorContainer(DockerImageName.parse("gcr.io/google.com/cloudsdktool/cloud-sdk:367.0.0-emulators"));
        pubSubEmulator.start();
        hostPort = pubSubEmulator.getEmulatorEndpoint();
        channel = ManagedChannelBuilder.forTarget(hostPort).usePlaintext().build();
        channelProvider = FixedTransportChannelProvider.create(GrpcTransportChannel.create(channel));
        senderTarget = GooglePubSubSenderTarget.builder()
                .withPubSubEndpoint(hostPort)
                .withTopicName(TopicName.of(projectId, topicId))
                .build();
        createTopic();
        createSubscription();
        final RetrySettings retrySettings = RetrySettings.newBuilder()
                .setInitialRpcTimeout(Duration.ofMillis(600_000L))
                .setMaxRpcTimeout(Duration.ofMillis(600_000L))
                .setTotalTimeout(Duration.ofMillis(600_000L))
                .setMaxAttempts(1)
                .build();
        final BatchingSettings batchingSettings = BatchingSettings.newBuilder()
                .setElementCountThreshold(1024L)
                .setRequestByteThreshold(1L)
                .setDelayThreshold(Duration.ofMillis(1))
                .build();
        final FixedExecutorProvider executorProvider = FixedExecutorProvider.create(Executors.newScheduledThreadPool(4, new ThreadFactoryBuilder().setNameFormat("pubsub-publisher-%d").build()));
        GooglePubSubClientsPool clientsPool = new GooglePubSubClientsPool(credentialsProvider, executorProvider, retrySettings, batchingSettings, new GooglePubSubMessages(new GooglePubSubMetadataAppender()), channelProvider);
        messageSender = new GooglePubSubMessageSender(senderTarget, clientsPool);
    }

    @AfterClass
    public static void stopContainer() {
        pubSubEmulator.stop();
        channel.shutdown();
    }

    @Test
    public void shouldConsumeMessagesOnMultipleSubscriptions() throws IOException {
        // given
        final Message message = new Message(
                "id",
                topicId,
                "hello world".getBytes(StandardCharsets.UTF_8),
                ContentType.JSON,
                Optional.empty(),
                System.currentTimeMillis(),
                System.currentTimeMillis(),
                new PartitionOffset(KafkaTopicName.valueOf(topicId), 0, 1),
                System.currentTimeMillis(),
                Maps.newHashMap(),
                Lists.newArrayList(),
                subscriptionId,
                false
        );

        // when
        messageSender.send(message);

        // then
        SubscriberStubSettings subscriberStubSettings =
                SubscriberStubSettings.newBuilder()
                        .setTransportChannelProvider(channelProvider)
                        .setCredentialsProvider(credentialsProvider)
                        .build();
        try (SubscriberStub subscriber = GrpcSubscriberStub.create(subscriberStubSettings)) {
            PullRequest pullRequest = PullRequest.newBuilder()
                    .setMaxMessages(1)
                    .setSubscription(ProjectSubscriptionName.format(projectId, subscriptionId))
                    .build();
            PullResponse pullResponse = subscriber.pullCallable().call(pullRequest);

            assertThat(pullResponse.getReceivedMessagesList()).hasSize(1);
            assertThat(pullResponse.getReceivedMessages(0).getMessage().getData().toStringUtf8()).isEqualTo("hello world");
        }
    }

//    @Test
//    @Ignore
//    public void shouldSendMessagesToPubSub() throws IOException {
//        SubscriberStubSettings subscriberStubSettings =
//                SubscriberStubSettings.newBuilder()
//                        .setTransportChannelProvider(channelProvider)
//                        .setCredentialsProvider(credentialsProvider)
//                        .build();
//        try (SubscriberStub subscriber = GrpcSubscriberStub.create(subscriberStubSettings)) {
//            PullRequest pullRequest = PullRequest.newBuilder()
//                    .setMaxMessages(1)
//                    .setSubscription(ProjectSubscriptionName.format(projectId, subscriptionId))
//                    .build();
//            PullResponse pullResponse = subscriber.pullCallable().call(pullRequest);
//
//            assertThat(pullResponse.getReceivedMessagesList()).hasSize(1);
//            assertThat(pullResponse.getReceivedMessages(0).getMessage().getData().toStringUtf8()).isEqualTo("hello world");
//        }
//    }

    private static void createTopic() throws IOException {
        TopicAdminSettings topicAdminSettings = TopicAdminSettings.newBuilder()
                .setTransportChannelProvider(channelProvider)
                .setCredentialsProvider(credentialsProvider)
                .build();
        try (TopicAdminClient topicAdminClient = TopicAdminClient.create(topicAdminSettings)) {
            TopicName topicName = TopicName.of(projectId, topicId);
            topicAdminClient.createTopic(topicName);
        }
    }

    private static void createSubscription() throws IOException {
        SubscriptionAdminSettings subscriptionAdminSettings = SubscriptionAdminSettings.newBuilder()
                .setTransportChannelProvider(channelProvider)
                .setCredentialsProvider(credentialsProvider)
                .build();
        SubscriptionAdminClient subscriptionAdminClient = SubscriptionAdminClient.create(subscriptionAdminSettings);
        ProjectSubscriptionName subscriptionName = ProjectSubscriptionName.of(projectId, subscriptionId);
        subscriptionAdminClient.createSubscription(subscriptionName, TopicName.of(projectId, topicId), PushConfig.getDefaultInstance(), 10);
    }
}
