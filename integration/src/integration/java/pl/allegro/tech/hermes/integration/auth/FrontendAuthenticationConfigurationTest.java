package pl.allegro.tech.hermes.integration.auth;

import avro.shaded.com.google.common.collect.Lists;
import io.undertow.security.impl.BasicAuthenticationMechanism;
import io.undertow.util.StatusCodes;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.frontend.HermesFrontend;
import pl.allegro.tech.hermes.frontend.server.auth.AuthenticationConfiguration;
import pl.allegro.tech.hermes.frontend.server.HermesServer;
import pl.allegro.tech.hermes.integration.IntegrationTest;
import pl.allegro.tech.hermes.test.helper.config.MutableConfigFactory;
import pl.allegro.tech.hermes.test.helper.endpoint.HermesPublisher;
import pl.allegro.tech.hermes.test.helper.message.TestMessage;
import pl.allegro.tech.hermes.test.helper.util.Ports;

import javax.ws.rs.core.Response;
import java.util.Map;

import static javax.ws.rs.core.Response.Status.Family.SUCCESSFUL;
import static pl.allegro.tech.hermes.integration.auth.SingleUserAwareIdentityManager.getHeadersWithAuthentication;
import static pl.allegro.tech.hermes.integration.test.HermesAssertions.assertThat;

public class FrontendAuthenticationConfigurationTest extends IntegrationTest {

    public static final int FRONTEND_PORT = Ports.nextAvailable();
    public static final String FRONTEND_URL = "http://127.0.0.1:" + FRONTEND_PORT;

    private static final String USERNAME = "someUser";
    private static final String PASSWORD = "somePassword123";
    private static final String MESSAGE = TestMessage.of("hello", "world").body();

    protected HermesPublisher publisher;
    protected HermesServer hermesServer;

    private HermesFrontend hermesFrontend;

    @BeforeClass
    public void setup() throws Exception {
        ConfigFactory configFactory = new MutableConfigFactory()
                .overrideProperty(Configs.FRONTEND_PORT, FRONTEND_PORT)
                .overrideProperty(Configs.FRONTEND_SSL_ENABLED, false)
                .overrideProperty(Configs.FRONTEND_AUTHENTICATION_MODE, "constraint_driven")
                .overrideProperty(Configs.FRONTEND_AUTHENTICATION_ENABLED, true);

        AuthenticationConfiguration authConfig = new AuthenticationConfiguration(
                exchange -> true,
                Lists.newArrayList(new BasicAuthenticationMechanism("basicAuthRealm")),
                new SingleUserAwareIdentityManager(USERNAME, PASSWORD));

        hermesFrontend = HermesFrontend.frontend()
                .withBinding(configFactory, ConfigFactory.class)
                .withAuthenticationConfiguration(authConfig)
                .build();

        hermesFrontend.start();

        hermesServer = hermesFrontend.getService(HermesServer.class);
        publisher = new HermesPublisher(FRONTEND_URL);

        operations.buildTopic("someGroup", "topicWithAuthorization");
    }

    @AfterClass
    public void tearDown() throws InterruptedException {
        hermesFrontend.stop();
    }

    @Test
    public void shouldAuthenticateUsingBasicAuth() throws Throwable {
        //given
        Map<String, String> headers = getHeadersWithAuthentication(USERNAME, PASSWORD);

        //when
        Response response = publisher.publish("someGroup.topicWithAuthorization", MESSAGE, headers);

        //then
        assertThat(response.getStatusInfo().getFamily()).isEqualTo(SUCCESSFUL);
    }

    @Test
    public void shouldNotAuthenticateUserWithInvalidCredentials() throws Throwable {
        //given
        Map<String, String> headers = getHeadersWithAuthentication(USERNAME, "someInvalidPassword");

        //when
        Response response = publisher.publish("someGroup.topicWithAuthorization", MESSAGE, headers);

        //then
        assertThat(response.getStatus()).isEqualTo(StatusCodes.UNAUTHORIZED);
    }

    @Test
    public void shouldNotAuthenticateUserWithoutCredentials() throws Throwable {
        //when
        Response response = publisher.publish("someGroup.topicWithAuthorization", MESSAGE);

        //then
        assertThat(response.getStatus()).isEqualTo(StatusCodes.UNAUTHORIZED);
    }
}
