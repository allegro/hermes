package pl.allegro.tech.hermes.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Files;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import pl.allegro.tech.hermes.api.ContentType;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.message.wrapper.CompositeMessageContentWrapper;
import pl.allegro.tech.hermes.common.message.wrapper.JsonMessageContentWrapper;
import pl.allegro.tech.hermes.frontend.buffer.BackupFilesManager;
import pl.allegro.tech.hermes.frontend.buffer.MessageRepository;
import pl.allegro.tech.hermes.frontend.buffer.chronicle.ChronicleMapMessageRepository;
import pl.allegro.tech.hermes.frontend.publishing.message.JsonMessage;
import pl.allegro.tech.hermes.frontend.publishing.message.MessageIdGenerator;
import pl.allegro.tech.hermes.integration.env.FrontendStarter;
import pl.allegro.tech.hermes.integration.env.SharedServices;
import pl.allegro.tech.hermes.test.helper.endpoint.HermesAPIOperations;
import pl.allegro.tech.hermes.test.helper.endpoint.HermesEndpoints;
import pl.allegro.tech.hermes.test.helper.endpoint.HermesPublisher;
import pl.allegro.tech.hermes.test.helper.endpoint.RemoteServiceEndpoint;
import pl.allegro.tech.hermes.test.helper.endpoint.Waiter;
import pl.allegro.tech.hermes.test.helper.util.Ports;

import java.io.File;
import java.time.Clock;
import java.util.Collections;

import static com.jayway.awaitility.Awaitility.await;
import static java.nio.charset.Charset.defaultCharset;
import static java.time.Instant.now;
import static java.util.concurrent.TimeUnit.SECONDS;
import static javax.ws.rs.core.Response.Status.ACCEPTED;
import static javax.ws.rs.core.Response.Status.CREATED;
import static org.assertj.core.api.Assertions.assertThat;
import static pl.allegro.tech.hermes.frontend.FrontendConfigurationProperties.KAFKA_BROKER_LIST;
import static pl.allegro.tech.hermes.frontend.FrontendConfigurationProperties.MESSAGES_LOCAL_STORAGE_DIRECTORY;
import static pl.allegro.tech.hermes.frontend.FrontendConfigurationProperties.MESSAGES_LOCAL_STORAGE_ENABLED;
import static pl.allegro.tech.hermes.frontend.FrontendConfigurationProperties.SCHEMA_REPOSITORY_SERVER_URL;
import static pl.allegro.tech.hermes.frontend.FrontendConfigurationProperties.ZOOKEEPER_CONNECTION_STRING;
import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.randomTopic;

public class MessageBufferLoadingTest extends IntegrationTest {
    private static final int ENTRIES = 100;
    private static final int AVERAGE_MESSAGE_SIZE = 600;

    private final HermesEndpoints management = new HermesEndpoints(MANAGEMENT_ENDPOINT_URL, CONSUMER_ENDPOINT_URL);
    private final HermesAPIOperations operations = new HermesAPIOperations(management, new Waiter(management));

    private RemoteServiceEndpoint remoteService;

    @BeforeMethod
    public void setup() {
        remoteService = new RemoteServiceEndpoint(SharedServices.services().serviceMock());
    }

    @Test
    public void shouldBackupMessage() throws Exception {
        // setup
        int frontendPort = Ports.nextAvailable();
        HermesPublisher publisher = new HermesPublisher("http://localhost:" + frontendPort + "/");
        String backupStorageDir = Files.createTempDir().getAbsolutePath();

        Topic topic = randomTopic("backupGroup", "uniqueTopic").build();
        operations.buildTopic(topic);

        FrontendStarter frontend = FrontendStarter.withCommonIntegrationTestConfig(frontendPort, false);
        frontend.overrideProperty(MESSAGES_LOCAL_STORAGE_DIRECTORY, backupStorageDir);
        frontend.overrideProperty(KAFKA_BROKER_LIST, kafkaClusterOne.getBootstrapServersForExternalClients());
        frontend.overrideProperty(ZOOKEEPER_CONNECTION_STRING, hermesZookeeperOne.getConnectionString());
        frontend.overrideProperty(SCHEMA_REPOSITORY_SERVER_URL, schemaRegistry.getUrl());
        frontend.overrideProperty(MESSAGES_LOCAL_STORAGE_ENABLED, true);
        frontend.start();

        try {
            //given
            final ChronicleMapMessageRepository backupRepository = createBackupRepository(backupStorageDir);

            await().atMost(5, SECONDS).until(() ->
                    assertThat(publisher.publish(topic.getQualifiedName(), "message").getStatus())
                            .isEqualTo(CREATED.getStatusCode()));

            // when
            kafkaClusterOne.cutOffConnectionsBetweenBrokersAndClients();
            assertThat(publisher.publish(topic.getQualifiedName(), "message").getStatus()).isEqualTo(ACCEPTED.getStatusCode());

            // then
            await().atMost(10, SECONDS).until(() -> assertThat(backupRepository.findAll()).hasSize(1));

        } finally {
            // after
            kafkaClusterOne.restoreConnectionsBetweenBrokersAndClients();
            frontend.stop();
        }
    }

    @Test
    public void shouldLoadMessageFromBackupStorage() throws Exception {
        // given
        String tempDirPath = Files.createTempDir().getAbsolutePath();
        Topic topic = randomTopic("backupGroup", "topic").withContentType(ContentType.JSON).build();
        backupFileWithOneMessage(tempDirPath, topic);

        operations.createSubscription(operations.buildTopic(topic), "subscription", remoteService.getUrl());

        remoteService.expectMessages("message");

        FrontendStarter frontend = FrontendStarter.withCommonIntegrationTestConfig(Ports.nextAvailable(), false);
        frontend.overrideProperty(MESSAGES_LOCAL_STORAGE_DIRECTORY, tempDirPath);
        frontend.overrideProperty(KAFKA_BROKER_LIST, kafkaClusterOne.getBootstrapServersForExternalClients());
        frontend.overrideProperty(ZOOKEEPER_CONNECTION_STRING, hermesZookeeperOne.getConnectionString());
        frontend.overrideProperty(SCHEMA_REPOSITORY_SERVER_URL, schemaRegistry.getUrl());

        // when
        frontend.start();

        // then
        remoteService.waitUntilReceived();

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

        messageRepository.save(new JsonMessage(messageId, content, timestamp, null), topic);
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

