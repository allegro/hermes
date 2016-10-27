package pl.allegro.tech.hermes.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Files;
import org.apache.commons.io.FileUtils;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import pl.allegro.tech.hermes.api.ContentType;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.message.wrapper.JsonMessageContentWrapper;
import pl.allegro.tech.hermes.common.message.wrapper.MessageContentWrapper;
import pl.allegro.tech.hermes.frontend.buffer.BackupFilesManager;
import pl.allegro.tech.hermes.frontend.buffer.MessageRepository;
import pl.allegro.tech.hermes.frontend.buffer.chronicle.ChronicleMapMessageRepository;
import pl.allegro.tech.hermes.frontend.publishing.message.JsonMessage;
import pl.allegro.tech.hermes.integration.env.FrontendStarter;
import pl.allegro.tech.hermes.integration.env.SharedServices;
import pl.allegro.tech.hermes.test.helper.endpoint.HermesAPIOperations;
import pl.allegro.tech.hermes.test.helper.endpoint.HermesEndpoints;
import pl.allegro.tech.hermes.test.helper.endpoint.HermesPublisher;
import pl.allegro.tech.hermes.test.helper.endpoint.RemoteServiceEndpoint;
import pl.allegro.tech.hermes.test.helper.endpoint.Waiter;
import pl.allegro.tech.hermes.test.helper.environment.KafkaStarter;
import pl.allegro.tech.hermes.test.helper.util.Ports;

import java.io.File;
import java.io.IOException;
import java.time.Clock;
import java.util.Collections;
import java.util.Properties;

import static com.jayway.awaitility.Awaitility.await;
import static java.nio.charset.Charset.defaultCharset;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;
import static java.util.concurrent.TimeUnit.SECONDS;
import static javax.ws.rs.core.Response.Status.ACCEPTED;
import static javax.ws.rs.core.Response.Status.CREATED;
import static org.assertj.core.api.Assertions.assertThat;
import static pl.allegro.tech.hermes.common.config.Configs.KAFKA_BROKER_LIST;
import static pl.allegro.tech.hermes.common.config.Configs.KAFKA_ZOOKEEPER_CONNECT_STRING;
import static pl.allegro.tech.hermes.common.config.Configs.MESSAGES_LOCAL_STORAGE_DIRECTORY;
import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.topic;

public class MessageBufferLoadingTest extends IntegrationTest {

    private static final int KAFKA_PORT = Ports.nextAvailable();
    private static final int FRONTEND_PORT = Ports.nextAvailable();

    private static final String FRONTEND_URL = "http://localhost:" + FRONTEND_PORT + "/";
    private static final String KAFKA_ZK_CONNECT_STRING = ZOOKEEPER_CONNECT_STRING + "/backupKafka";
    private static final String KAFKA_BROKERS_LIST = "localhost:" + KAFKA_PORT;

    private final HermesEndpoints management = new HermesEndpoints(MANAGEMENT_ENDPOINT_URL, CONSUMER_ENDPOINT_URL);
    private final HermesAPIOperations operations = new HermesAPIOperations(management, new Waiter(management));
    private final HermesPublisher publisher = new HermesPublisher(FRONTEND_URL);

    private RemoteServiceEndpoint remoteService;

    private File tempDir;

    @BeforeMethod
    public void setup() {
        tempDir = Files.createTempDir();
        remoteService = new RemoteServiceEndpoint(SharedServices.services().serviceMock());
    }

    @AfterMethod
    public void tearDown() throws IOException {
        FileUtils.deleteDirectory(tempDir);
    }


    @Test(enabled = false)
    public void shouldBackupMessage() throws Exception {
        // given
        KafkaStarter kafka = new KafkaStarter(kafkaProperties());
        kafka.start();

        operations.buildTopic("backupGroup", "uniqueTopic");

        FrontendStarter frontend = new FrontendStarter(FRONTEND_PORT, false);
        frontend.overrideProperty(KAFKA_BROKER_LIST, KAFKA_BROKERS_LIST);
        frontend.overrideProperty(KAFKA_ZOOKEEPER_CONNECT_STRING, KAFKA_ZK_CONNECT_STRING);
        frontend.start();

        try {
            ChronicleMapMessageRepository backupRepository = createBackupRepository(
                    frontend.config().getStringProperty(MESSAGES_LOCAL_STORAGE_DIRECTORY)
            );

            await().atMost(5, SECONDS).until(() ->
                    assertThat(publisher.publish("backupGroup.uniqueTopic", "message").getStatus())
                            .isEqualTo(CREATED.getStatusCode()));

            // when
            kafka.stop();
            assertThat(publisher.publish("backupGroup.uniqueTopic", "message").getStatus()).isEqualTo(ACCEPTED.getStatusCode());

            // then
            await().atMost(10, SECONDS).until(() -> assertThat(backupRepository.findAll()).hasSize(1));

        } finally {
            // after
            kafka.start();
            frontend.stop();
            kafka.stop();
        }
    }

    @Test
    public void shouldLoadMessageFromBackupStorage() throws Exception {
        // given
        Topic topic = topic("backupGroup", "topic").withContentType(ContentType.JSON).build();
        backupFileWithOneMessage(topic);

        operations.createSubscription(operations.buildTopic(topic), "subscription", HTTP_ENDPOINT_URL);

        remoteService.expectMessages("message");

        FrontendStarter frontend = new FrontendStarter(Ports.nextAvailable(), false);
        frontend.overrideProperty(MESSAGES_LOCAL_STORAGE_DIRECTORY, tempDir.getAbsolutePath());

        // when
        frontend.start();

        // then
        remoteService.waitUntilReceived();

        // after
        frontend.stop();
    }

    private File backupFileWithOneMessage(Topic topic) {
        File backup = new File(tempDir.getAbsoluteFile(), "hermes-buffer.dat");

        MessageRepository messageRepository = new ChronicleMapMessageRepository(backup);
        MessageContentWrapper wrapper = new MessageContentWrapper(new JsonMessageContentWrapper(CONFIG_FACTORY, new ObjectMapper()), null, null);

        String messageId = randomUUID().toString();
        long timestamp = now().toEpochMilli();
        byte[] content = wrapper.wrapJson("message".getBytes(defaultCharset()),
                messageId, timestamp, Collections.emptyMap());

        messageRepository.save(new JsonMessage(messageId, content, timestamp), topic);
        messageRepository.close();

        return backup;
    }

    private Properties kafkaProperties() {
        Properties properties = new Properties();
        properties.setProperty("port", String.valueOf(KAFKA_PORT));
        properties.setProperty("zookeeper.connect", KAFKA_ZK_CONNECT_STRING);
        properties.setProperty("broker.id", "0");
        properties.setProperty("log.dirs", tempDir.getAbsolutePath() + "/kafka-logs-it");

        return properties;
    }

    private ChronicleMapMessageRepository createBackupRepository(String storageDirPath) {
        return new ChronicleMapMessageRepository(
                new BackupFilesManager(storageDirPath, Clock.systemUTC()).getCurrentBackupFile()
        );
    }
}

