package pl.allegro.tech.hermes.integration;

import com.google.api.core.ApiFuture;
import com.google.api.gax.core.NoCredentialsProvider;
import com.google.api.gax.grpc.GrpcTransportChannel;
import com.google.api.gax.rpc.FixedTransportChannelProvider;
import com.google.api.gax.rpc.TransportChannelProvider;
import com.google.cloud.pubsub.v1.*;
import com.google.cloud.pubsub.v1.stub.GrpcSubscriberStub;
import com.google.cloud.pubsub.v1.stub.SubscriberStub;
import com.google.cloud.pubsub.v1.stub.SubscriberStubSettings;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.mockito.InjectMocks;
import org.testcontainers.containers.PubSubEmulatorContainer;
import org.testcontainers.utility.DockerImageName;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.consumers.consumer.Message;
import pl.allegro.tech.hermes.consumers.consumer.sender.pubsub.PubSubMessageSender;
import pl.allegro.tech.hermes.integration.env.SharedServices;
import pl.allegro.tech.hermes.integration.test.HermesAssertions;
import pl.allegro.tech.hermes.test.helper.endpoint.RemoteServiceEndpoint;
import pl.allegro.tech.hermes.test.helper.message.TestMessage;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static javax.ws.rs.core.Response.Status.CREATED;
import static org.assertj.core.api.Assertions.assertThat;
import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.randomTopic;

public class PubSubPublishingTest extends IntegrationTest {

    private static final String projectId = "sc-9620-datahub-staging-prod";
    private static final String topicId = "hermes-in-pubsub";
    private static final String subscriptionId = "hermes-in-pubsub-sub";

    private TransportChannelProvider channelProvider;
    private NoCredentialsProvider credentialsProvider;

    private RemoteServiceEndpoint remoteService;

    @InjectMocks
    private PubSubMessageSender messageSender;

    private static final Message SOME_MESSAGE = Message.message().withData("hello world".getBytes(StandardCharsets.UTF_8)).build();
    private final PubSubEmulatorContainer pubSubEmulator = new PubSubEmulatorContainer(DockerImageName.parse("gcr.io/google.com/cloudsdktool/cloud-sdk:367.0.0-emulators"));

    @BeforeMethod
    public void initializeAlways() {
        this.remoteService = new RemoteServiceEndpoint(SharedServices.services().serviceMock());
    }

    @BeforeClass
    public void initialize() throws IOException {
        String hostport = pubSubEmulator.getEmulatorEndpoint();
        ManagedChannel channel = ManagedChannelBuilder.forTarget(hostport).usePlaintext().build();
        try {
            channelProvider = FixedTransportChannelProvider.create(GrpcTransportChannel.create(channel));
            credentialsProvider = NoCredentialsProvider.create();

            createTopic();
            createSubscription();
        } finally {
            channel.shutdown();
        }
    }

    @Test
    public void shouldConsumeMessagesOnMultipleSubscriptions() {
        // given
        Topic topic = operations.buildTopic(randomTopic("publishAndConsumeGroup", "topic").build());
        operations.createSubscription(topic, "subscription", "pubsub://testsub");

        TestMessage message = TestMessage.of("hello", "world");
        remoteService.expectMessages(message.body());

        // when
        Response response = publisher.publish(topic.getQualifiedName(), message.body());

        // then
        HermesAssertions.assertThat(response).hasStatus(CREATED);
        remoteService.waitUntilReceived();
    }

    @Test
    public void shouldSendMessagesToPubSub() throws IOException {
        Publisher publisher = Publisher.newBuilder(TopicName.of(projectId, topicId))
                .setChannelProvider(channelProvider)
                .setCredentialsProvider(credentialsProvider)
                .build();
        messageSender.send(SOME_MESSAGE);

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

    private void createTopic() throws IOException {
        TopicAdminSettings topicAdminSettings = TopicAdminSettings.newBuilder()
                .setTransportChannelProvider(channelProvider)
                .setCredentialsProvider(credentialsProvider)
                .build();
        try (TopicAdminClient topicAdminClient = TopicAdminClient.create(topicAdminSettings)) {
            TopicName topicName = TopicName.of(projectId, topicId);
            topicAdminClient.createTopic(topicName);
        }
    }

    private void createSubscription() throws IOException {
        SubscriptionAdminSettings subscriptionAdminSettings = SubscriptionAdminSettings.newBuilder()
                .setTransportChannelProvider(channelProvider)
                .setCredentialsProvider(credentialsProvider)
                .build();
        SubscriptionAdminClient subscriptionAdminClient = SubscriptionAdminClient.create(subscriptionAdminSettings);
        ProjectSubscriptionName subscriptionName = ProjectSubscriptionName.of(projectId, subscriptionId);
        subscriptionAdminClient.createSubscription(subscriptionName, TopicName.of(projectId, topicId), PushConfig.getDefaultInstance(), 10);
    }
}
