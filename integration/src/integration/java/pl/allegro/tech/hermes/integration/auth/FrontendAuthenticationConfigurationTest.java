package pl.allegro.tech.hermes.integration.auth;

import io.undertow.util.StatusCodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import pl.allegro.tech.hermes.frontend.FrontendConfigurationProperties;
import pl.allegro.tech.hermes.frontend.server.HermesServer;
import pl.allegro.tech.hermes.integration.IntegrationTest;
import pl.allegro.tech.hermes.integration.env.FrontendStarter;
import pl.allegro.tech.hermes.test.helper.endpoint.HermesPublisher;
import pl.allegro.tech.hermes.test.helper.message.TestMessage;
import pl.allegro.tech.hermes.test.helper.util.Ports;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.Response.Status.Family.SUCCESSFUL;
import static pl.allegro.tech.hermes.frontend.FrontendConfigurationProperties.FRONTEND_AUTHENTICATION_ENABLED;
import static pl.allegro.tech.hermes.frontend.FrontendConfigurationProperties.FRONTEND_SSL_ENABLED;
import static pl.allegro.tech.hermes.frontend.FrontendConfigurationProperties.KAFKA_BROKER_LIST;
import static pl.allegro.tech.hermes.frontend.FrontendConfigurationProperties.SCHEMA_REPOSITORY_SERVER_URL;
import static pl.allegro.tech.hermes.frontend.FrontendConfigurationProperties.ZOOKEEPER_CONNECTION_STRING;
import static pl.allegro.tech.hermes.integration.auth.SingleUserAwareIdentityManager.getHeadersWithAuthentication;
import static pl.allegro.tech.hermes.integration.test.HermesAssertions.assertThat;

public class FrontendAuthenticationConfigurationTest extends IntegrationTest {

    public static final int FRONTEND_PORT = Ports.nextAvailable();
    public static final String FRONTEND_URL = "http://127.0.0.1:" + FRONTEND_PORT;

    private static final Logger logger = LoggerFactory.getLogger(FrontendAuthenticationConfigurationTest.class);
    private static String USERNAME;
    private static String PASSWORD;
    private static final String MESSAGE = TestMessage.of("hello", "world").body();

    protected HermesPublisher publisher;
    protected HermesServer hermesServer;

    private FrontendStarter frontendStarter;

    @BeforeClass
    public void setup() throws Exception {
        loadCredentials();
        frontendStarter = new FrontendStarter(FRONTEND_PORT);
        frontendStarter.addSpringProfiles("authRequired");
        frontendStarter.overrideProperty(FrontendConfigurationProperties.FRONTEND_PORT, FRONTEND_PORT);
        frontendStarter.overrideProperty(FRONTEND_SSL_ENABLED, false);
        frontendStarter.overrideProperty(FRONTEND_AUTHENTICATION_ENABLED, true);
        frontendStarter.overrideProperty(KAFKA_BROKER_LIST, kafkaClusterOne.getBootstrapServersForExternalClients());
        frontendStarter.overrideProperty(ZOOKEEPER_CONNECTION_STRING, hermesZookeeperOne.getConnectionString());
        frontendStarter.overrideProperty(SCHEMA_REPOSITORY_SERVER_URL, schemaRegistry.getUrl());

        frontendStarter.start();

        hermesServer = frontendStarter.instance().getBean(HermesServer.class);
        publisher = new HermesPublisher(FRONTEND_URL);
    }

    @BeforeMethod
    public void after() {
        operations.buildTopic("someGroup", "topicWithAuthorization");
    }

    @AfterClass
    public void tearDown() throws Exception {
        frontendStarter.stop();
    }

    @Test
    public void shouldAuthenticateUsingBasicAuth() {
        //given
        Map<String, String> headers = getHeadersWithAuthentication(USERNAME, PASSWORD);

        //when
        Response response = publisher.publish("someGroup.topicWithAuthorization", MESSAGE, headers);

        //then
        logger.info("Expecting SUCCESSFUL status. Actual {}", response.getStatusInfo().getStatusCode());
        assertThat(response.getStatusInfo().getFamily()).isEqualTo(SUCCESSFUL);
    }

    @Test
    public void shouldNotAuthenticateUserWithInvalidCredentials() {
        //given
        Map<String, String> headers = getHeadersWithAuthentication(USERNAME, "someInvalidPassword");

        //when
        Response response = publisher.publish("someGroup.topicWithAuthorization", MESSAGE, headers);

        //then
        assertThat(response.getStatus()).isEqualTo(StatusCodes.UNAUTHORIZED);
    }

    @Test
    public void shouldNotAuthenticateUserWithoutCredentials() {
        //when
        Response response = publisher.publish("someGroup.topicWithAuthorization", MESSAGE);

        //then
        assertThat(response.getStatus()).isEqualTo(StatusCodes.UNAUTHORIZED);
    }

    private static void loadCredentials() throws IOException {
        Properties properties = new Properties();
        try {
            properties.load(Thread.currentThread().getContextClassLoader().getResourceAsStream("application-auth.properties"));
        } catch (IOException e) {
            throw new IOException("Failed to load 'application-auth.properties' file", e);
        }
        USERNAME = properties.getProperty("auth.username");
        PASSWORD = properties.getProperty("auth.password");
    }
}
