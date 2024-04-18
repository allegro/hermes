package pl.allegro.tech.hermes.integrationtests;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Files;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.message.wrapper.CompositeMessageContentWrapper;
import pl.allegro.tech.hermes.common.message.wrapper.JsonMessageContentWrapper;
import pl.allegro.tech.hermes.frontend.buffer.BackupFilesManager;
import pl.allegro.tech.hermes.frontend.buffer.MessageRepository;
import pl.allegro.tech.hermes.frontend.buffer.chronicle.ChronicleMapMessageRepository;
import pl.allegro.tech.hermes.frontend.publishing.message.JsonMessage;
import pl.allegro.tech.hermes.frontend.publishing.message.MessageIdGenerator;
import pl.allegro.tech.hermes.integrationtests.setup.HermesConsumersTestApp;
import pl.allegro.tech.hermes.integrationtests.setup.HermesFrontendTestApp;
import pl.allegro.tech.hermes.integrationtests.setup.HermesManagementExtension;
import pl.allegro.tech.hermes.integrationtests.setup.InfrastructureExtension;
import pl.allegro.tech.hermes.integrationtests.subscriber.TestSubscriber;
import pl.allegro.tech.hermes.integrationtests.subscriber.TestSubscribersExtension;
import pl.allegro.tech.hermes.test.helper.client.integration.FrontendTestClient;

import java.io.File;
import java.time.Clock;
import java.util.Collections;

import static jakarta.ws.rs.core.Response.Status.ACCEPTED;
import static java.nio.charset.Charset.defaultCharset;
import static java.time.Instant.now;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static pl.allegro.tech.hermes.api.ContentType.JSON;
import static pl.allegro.tech.hermes.frontend.FrontendConfigurationProperties.MESSAGES_LOCAL_STORAGE_DIRECTORY;
import static pl.allegro.tech.hermes.frontend.FrontendConfigurationProperties.MESSAGES_LOCAL_STORAGE_ENABLED;
import static pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder.subscription;
import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.topicWithRandomName;

public class MessageBufferLoadingTest {

    private static final int ENTRIES = 100;
    private static final int AVERAGE_MESSAGE_SIZE = 600;

    @Order(0)
    @RegisterExtension
    public static InfrastructureExtension infra = new InfrastructureExtension();

    @Order(1)
    @RegisterExtension
    public static HermesManagementExtension management = new HermesManagementExtension(infra);

    private static final HermesConsumersTestApp consumers = new HermesConsumersTestApp(infra.hermesZookeeper(), infra.kafka(), infra.schemaRegistry());

    @BeforeAll
    public static void setup() {
        consumers.start();
    }

    @AfterAll
    public static void clean() {
        consumers.stop();
    }

    @RegisterExtension
    public static final TestSubscribersExtension subscribers = new TestSubscribersExtension();

    @Test
    public void shouldBackupMessage() {
        // setup
        String backupStorageDir = Files.createTempDir().getAbsolutePath();
        HermesFrontendTestApp frontend = new HermesFrontendTestApp(infra.hermesZookeeper(), infra.kafka(), infra.schemaRegistry());
        frontend.withProperty(MESSAGES_LOCAL_STORAGE_DIRECTORY, backupStorageDir);
        frontend.withProperty(MESSAGES_LOCAL_STORAGE_ENABLED, true);
        frontend.start();

        FrontendTestClient publisher = new FrontendTestClient(frontend.getPort());

        Topic topic = management.initHelper().createTopic(topicWithRandomName().build());

        try {
            //given
            final ChronicleMapMessageRepository backupRepository = createBackupRepository(backupStorageDir);

            publisher.publishUntilSuccess(topic.getQualifiedName(), "message");

            // when
            infra.kafka().cutOffConnectionsBetweenBrokersAndClients();

            publisher.publishUntilStatus(topic.getQualifiedName(), "message", ACCEPTED.getStatusCode());

            // then
            await().atMost(10, SECONDS).untilAsserted(() -> assertThat(backupRepository.findAll()).hasSize(1));

        } finally {
            // after
            infra.kafka().restoreConnectionsBetweenBrokersAndClients();
            frontend.stop();
        }
    }

    @Test
    public void shouldLoadMessageFromBackupStorage() {
        // given
        String tempDirPath = Files.createTempDir().getAbsolutePath();
        Topic topic = management.initHelper().createTopic(topicWithRandomName().withContentType(JSON).build());
        backupFileWithOneMessage(tempDirPath, topic);

        TestSubscriber subscriber = subscribers.createSubscriber();

        management.initHelper().createSubscription(
                subscription(topic.getQualifiedName(), "subscription", subscriber.getEndpoint()).build()
        );

        HermesFrontendTestApp frontend = new HermesFrontendTestApp(infra.hermesZookeeper(), infra.kafka(), infra.schemaRegistry());
        frontend.withProperty(MESSAGES_LOCAL_STORAGE_DIRECTORY, tempDirPath);

        // when
        frontend.start();

        // then
        subscriber.waitUntilReceived("message");

        // after
        frontend.stop();
    }

    private void backupFileWithOneMessage(String tempDirPath, Topic topic) {
        File backup = new File(tempDirPath, "hermes-buffer-v3.dat");

        MessageRepository messageRepository = new ChronicleMapMessageRepository(backup, ENTRIES, AVERAGE_MESSAGE_SIZE);
        JsonMessageContentWrapper contentWrapper = new JsonMessageContentWrapper("message", "metadata", new ObjectMapper());

        CompositeMessageContentWrapper wrapper = new CompositeMessageContentWrapper(contentWrapper, null, null, null, null, null);

        String messageId = MessageIdGenerator.generate();
        long timestamp = now().toEpochMilli();
        byte[] content = wrapper.wrapJson("message".getBytes(defaultCharset()),
                messageId, timestamp, Collections.emptyMap());

        messageRepository.save(new JsonMessage(messageId, content, timestamp, null, Collections.emptyMap()), topic);
        messageRepository.close();

    }

    private ChronicleMapMessageRepository createBackupRepository(String storageDirPath) {
        return new ChronicleMapMessageRepository(
                new BackupFilesManager(storageDirPath, Clock.systemUTC()).getCurrentBackupFile(),
                ENTRIES,
                AVERAGE_MESSAGE_SIZE
        );
    }
}
