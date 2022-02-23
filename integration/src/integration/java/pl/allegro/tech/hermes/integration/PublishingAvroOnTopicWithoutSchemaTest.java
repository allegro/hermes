package pl.allegro.tech.hermes.integration;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.google.common.io.Files;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import pl.allegro.tech.hermes.api.ErrorDescription;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.frontend.HermesFrontend;
import pl.allegro.tech.hermes.integration.env.FrontendStarter;
import pl.allegro.tech.hermes.test.helper.avro.AvroUser;
import pl.allegro.tech.hermes.test.helper.avro.AvroUserSchemaLoader;
import pl.allegro.tech.hermes.test.helper.config.MutableConfigFactory;
import pl.allegro.tech.hermes.test.helper.endpoint.HermesPublisher;
import pl.allegro.tech.hermes.test.helper.util.Ports;

import javax.ws.rs.core.Response;

import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static pl.allegro.tech.hermes.api.ContentType.AVRO;
import static pl.allegro.tech.hermes.api.ErrorCode.SCHEMA_COULD_NOT_BE_LOADED;
import static pl.allegro.tech.hermes.api.TopicWithSchema.topicWithSchema;
import static pl.allegro.tech.hermes.common.config.Configs.FRONTEND_FORCE_TOPIC_MAX_MESSAGE_SIZE;
import static pl.allegro.tech.hermes.common.config.Configs.FRONTEND_GRACEFUL_SHUTDOWN_ENABLED;
import static pl.allegro.tech.hermes.common.config.Configs.FRONTEND_PORT;
import static pl.allegro.tech.hermes.common.config.Configs.FRONTEND_THROUGHPUT_FIXED_MAX;
import static pl.allegro.tech.hermes.common.config.Configs.FRONTEND_THROUGHPUT_TYPE;
import static pl.allegro.tech.hermes.common.config.Configs.SCHEMA_CACHE_ENABLED;
import static pl.allegro.tech.hermes.integration.test.HermesAssertions.assertThat;
import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.randomTopic;

public class PublishingAvroOnTopicWithoutSchemaTest extends IntegrationTest {

    public static final int FRONTEND_PORT = Ports.nextAvailable();

    public static final String FRONTEND_URL = "http://127.0.0.1:" + FRONTEND_PORT;

    protected HermesPublisher publisher;

//    private HermesFrontend hermesFrontend;
    private FrontendStarter frontendStarter;

    private WireMockServer emptySchemaRegistryMock = new WireMockServer(Ports.nextAvailable());

    @BeforeClass
    public void setup() throws Exception {
        emptySchemaRegistryMock.start();
        frontendStarter = new FrontendStarter(FRONTEND_PORT);
        frontendStarter.overrideProperty(Configs.FRONTEND_PORT, FRONTEND_PORT);
        frontendStarter.overrideProperty(Configs.SCHEMA_REPOSITORY_SERVER_URL, "http://localhost:" + emptySchemaRegistryMock.port());
        frontendStarter.overrideProperty(Configs.KAFKA_AUTHORIZATION_ENABLED, false);
        frontendStarter.overrideProperty(Configs.KAFKA_BROKER_LIST, kafkaClusterOne.getBootstrapServersForExternalClients());
        frontendStarter.overrideProperty(Configs.ZOOKEEPER_CONNECT_STRING, hermesZookeeperOne.getConnectionString());
        frontendStarter.overrideProperty(Configs.FRONTEND_SSL_ENABLED, false);
        frontendStarter.overrideProperty(Configs.MESSAGES_LOCAL_STORAGE_DIRECTORY, Files.createTempDir().getAbsolutePath());

//        ConfigFactory configFactory = new MutableConfigFactory()
//                .overrideProperty(Configs.FRONTEND_PORT, FRONTEND_PORT)
//                .overrideProperty(Configs.SCHEMA_REPOSITORY_SERVER_URL, "http://localhost:" + emptySchemaRegistryMock.port())
//                .overrideProperty(Configs.KAFKA_AUTHORIZATION_ENABLED, false)
//                .overrideProperty(Configs.KAFKA_BROKER_LIST, kafkaClusterOne.getBootstrapServersForExternalClients())
//                .overrideProperty(Configs.ZOOKEEPER_CONNECT_STRING, hermesZookeeperOne.getConnectionString())
//                .overrideProperty(Configs.FRONTEND_SSL_ENABLED, false)
//                .overrideProperty(Configs.MESSAGES_LOCAL_STORAGE_DIRECTORY, Files.createTempDir().getAbsolutePath());

//        hermesFrontend = HermesFrontend.frontend()
//                .withBinding(configFactory, ConfigFactory.class)
//                .build();

//        hermesFrontend.start();
        frontendStarter.start();
        publisher = new HermesPublisher(FRONTEND_URL);
    }

    @AfterClass
    public void tearDown() throws Exception {
        emptySchemaRegistryMock.stop();
//        hermesFrontend.stop();
        frontendStarter.stop();
    }

    @Test
    public void shouldReturnServerInternalErrorResponseOnMissingSchema() {
        // given
        Topic topic = randomTopic("avro", "topicWithoutSchema").withContentType(AVRO).build();
        operations.buildTopicWithSchema(topicWithSchema(topic, AvroUserSchemaLoader.load().toString()));

        // when
        String message = new AvroUser("Bob", 50, "blue").asJson();
        Response response = publisher.publish(topic.getQualifiedName(), message);

        // then
        assertThat(response).hasStatus(INTERNAL_SERVER_ERROR);
        assertThat(response.readEntity(ErrorDescription.class).getCode()).isEqualTo(SCHEMA_COULD_NOT_BE_LOADED);
    }
}
